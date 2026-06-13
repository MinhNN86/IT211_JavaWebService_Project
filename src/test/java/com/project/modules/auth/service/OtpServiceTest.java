package com.project.modules.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.project.common.exception.BadRequestException;
import com.project.config.MailProperties;
import com.project.modules.auth.repository.RefreshTokenRepository;
import com.project.modules.auth.service.impl.OtpServiceImpl;
import com.project.modules.user.entity.User;
import com.project.modules.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {
    private static final String EMAIL = "user@test.local";

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private MailService mailService;

    private OtpService service;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        MailProperties mailProperties = new MailProperties("test@localhost", 6, 5, 5, 60);
        service = new OtpServiceImpl(redisTemplate, userRepository, passwordEncoder, refreshTokenRepository,
                mailService, mailProperties);
    }

    @Test
    void issueOtpForExistingUserStoresHashAndSendsMail() {
        when(redisTemplate.hasKey(cooldownKey())).thenReturn(false);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));

        service.issueOtp(EMAIL);

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> storedHashCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(otpKey()), storedHashCaptor.capture(), eq(Duration.ofMinutes(5)));
        verify(valueOperations).set(eq(attemptsKey()), eq("0"), eq(Duration.ofMinutes(5)));
        verify(valueOperations).set(eq(cooldownKey()), eq("1"), eq(Duration.ofSeconds(60)));
        verify(mailService).sendOtp(eq(EMAIL), otpCaptor.capture());

        assertThat(otpCaptor.getValue()).matches("\\d{6}");
        assertThat(storedHashCaptor.getValue()).isEqualTo(sha256(otpCaptor.getValue()));
    }

    @Test
    void issueOtpForUnknownEmailDoesNotThrowOrSendMail() {
        when(redisTemplate.hasKey(cooldownKey())).thenReturn(false);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        service.issueOtp(EMAIL);

        verify(valueOperations).set(eq(cooldownKey()), eq("1"), eq(Duration.ofSeconds(60)));
        verify(valueOperations, never()).set(eq(otpKey()), anyString(), any(Duration.class));
        verify(valueOperations, never()).set(eq(attemptsKey()), anyString(), any(Duration.class));
        verifyNoInteractions(mailService);
    }

    @Test
    void verifyAndResetWithValidOtpChangesPasswordAndRevokesRefreshTokens() {
        User user = user();
        when(valueOperations.get(otpKey())).thenReturn(sha256("123456"));
        when(valueOperations.get(attemptsKey())).thenReturn("0");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newsecret")).thenReturn("encoded");

        service.verifyAndReset(EMAIL, "123456", "newsecret");

        verify(valueOperations).increment(attemptsKey());
        assertThat(user.getPassword()).isEqualTo("encoded");
        verify(userRepository).save(user);
        verify(refreshTokenRepository).revokeByUserId(user.getId());
        verify(redisTemplate).delete(otpKey());
        verify(redisTemplate).delete(attemptsKey());
    }

    @Test
    void verifyAndResetWithInvalidOtpIncrementsAttemptsAndRejectsRequest() {
        when(valueOperations.get(otpKey())).thenReturn(sha256("123456"));
        when(valueOperations.get(attemptsKey())).thenReturn("0");

        assertThatThrownBy(() -> service.verifyAndReset(EMAIL, "000000", "newsecret"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid OTP");

        verify(valueOperations).increment(attemptsKey());
        verify(userRepository, never()).findByEmail(EMAIL);
        verifyNoInteractions(passwordEncoder, refreshTokenRepository, mailService);
    }

    @Test
    void verifyAndResetRejectsAfterMaxAttempts() {
        when(valueOperations.get(otpKey())).thenReturn(sha256("123456"));
        when(valueOperations.get(attemptsKey())).thenReturn("5");

        assertThatThrownBy(() -> service.verifyAndReset(EMAIL, "123456", "newsecret"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Too many incorrect attempts; please request a new code");

        verify(valueOperations, never()).increment(attemptsKey());
        verify(userRepository, never()).findByEmail(EMAIL);
    }

    private User user() {
        return User.builder().id(UUID.randomUUID()).email(EMAIL).username("user").password("old").build();
    }

    private String otpKey() {
        return "otp:reset:" + sha256(EMAIL);
    }

    private String attemptsKey() {
        return "otp:attempts:" + sha256(EMAIL);
    }

    private String cooldownKey() {
        return "otp:cooldown:" + sha256(EMAIL);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }
}
