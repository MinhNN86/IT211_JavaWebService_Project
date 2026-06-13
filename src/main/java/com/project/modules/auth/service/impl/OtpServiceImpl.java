package com.project.modules.auth.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.common.exception.BadRequestException;
import com.project.config.MailProperties;
import com.project.modules.auth.repository.RefreshTokenRepository;
import com.project.modules.auth.service.MailService;
import com.project.modules.auth.service.OtpService;
import com.project.modules.user.entity.User;
import com.project.modules.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OtpServiceImpl implements OtpService {
    private static final String OTP_KEY_PREFIX = "otp:reset:";
    private static final String ATTEMPTS_KEY_PREFIX = "otp:attempts:";
    private static final String COOLDOWN_KEY_PREFIX = "otp:cooldown:";
    private static final String COOLDOWN_VALUE = "1";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MailService mailService;
    private final MailProperties mailProperties;

    @Override
    public void issueOtp(String email) {
        String emailHash = sha256(email);
        String cooldownKey = cooldownKey(emailHash);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new BadRequestException("Please wait before requesting another code");
        }
        if (mailProperties.resendCooldownSeconds() > 0) {
            redisTemplate.opsForValue().set(cooldownKey, COOLDOWN_VALUE,
                    Duration.ofSeconds(mailProperties.resendCooldownSeconds()));
        }

        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            return;
        }

        String otp = generateOtp();
        Duration otpTtl = Duration.ofMinutes(mailProperties.otpTtlMinutes());
        redisTemplate.opsForValue().set(otpKey(emailHash), sha256(otp), otpTtl);
        redisTemplate.opsForValue().set(attemptsKey(emailHash), "0", otpTtl);

        mailService.sendOtp(email, otp);
    }

    @Override
    public void verifyAndReset(String email, String otp, String newPassword) {
        String emailHash = sha256(email);
        String otpKey = otpKey(emailHash);
        String attemptsKey = attemptsKey(emailHash);

        String storedHash = redisTemplate.opsForValue().get(otpKey);
        if (storedHash == null) {
            throw new BadRequestException("OTP expired or not requested");
        }

        int attempts = parseAttempts(redisTemplate.opsForValue().get(attemptsKey));
        if (attempts >= mailProperties.otpMaxAttempts()) {
            throw new BadRequestException("Too many incorrect attempts; please request a new code");
        }
        redisTemplate.opsForValue().increment(attemptsKey);

        if (!sha256(otp).equals(storedHash)) {
            throw new BadRequestException("Invalid OTP");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("OTP expired or not requested"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokenRepository.revokeByUserId(user.getId());

        redisTemplate.delete(otpKey);
        redisTemplate.delete(attemptsKey);
    }

    private String generateOtp() {
        int upperBound = (int) Math.pow(10, mailProperties.otpLength());
        return String.format("%0" + mailProperties.otpLength() + "d", SECURE_RANDOM.nextInt(upperBound));
    }

    private int parseAttempts(String attempts) {
        if (attempts == null || attempts.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(attempts);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String otpKey(String emailHash) {
        return OTP_KEY_PREFIX + emailHash;
    }

    private String attemptsKey(String emailHash) {
        return ATTEMPTS_KEY_PREFIX + emailHash;
    }

    private String cooldownKey(String emailHash) {
        return COOLDOWN_KEY_PREFIX + emailHash;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }
}
