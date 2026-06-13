package com.project.modules.auth.service.impl;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.common.enums.RoleName;
import com.project.common.exception.BadRequestException;
import com.project.common.exception.ConflictException;
import com.project.common.exception.NotFoundException;
import com.project.common.exception.UnauthorizedException;
import com.project.common.util.SecurityUtils;
import com.project.modules.auth.dto.request.ChangePasswordRequest;
import com.project.modules.auth.dto.request.ForgotPasswordRequest;
import com.project.modules.auth.dto.request.LoginRequest;
import com.project.modules.auth.dto.request.LogoutRequest;
import com.project.modules.auth.dto.request.RefreshTokenRequest;
import com.project.modules.auth.dto.request.RegisterRequest;
import com.project.modules.auth.dto.request.ResetPasswordRequest;
import com.project.modules.auth.dto.response.AuthResponse;
import com.project.modules.auth.dto.response.RefreshResponse;
import com.project.modules.auth.entity.RefreshToken;
import com.project.modules.auth.repository.RefreshTokenRepository;
import com.project.modules.auth.service.AuthService;
import com.project.modules.auth.service.OtpService;
import com.project.modules.user.entity.User;
import com.project.modules.user.repository.UserRepository;
import com.project.security.jwt.JwtProperties;
import com.project.security.jwt.JwtTokenBlacklistService;
import com.project.security.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenBlacklistService tokenBlacklistService;
    private final JwtProperties jwtProperties;
    private final OtpService otpService;

    public AuthResponse register(RegisterRequest request) {
        boolean usernameExists = userRepository.existsByUsername(request.username());
        boolean emailExists = userRepository.existsByEmail(request.email());
        if (usernameExists || emailExists) {
            throw new ConflictException("Username or email already exists");
        }

        User newUser = User.builder().fullName(request.fullName()).username(request.username()).email(request.email())
                .password(passwordEncoder.encode(request.password())).phone(request.phone()).role(RoleName.CUSTOMER)
                .build();
        User savedUser = userRepository.save(newUser);
        return createAuthResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    request.username(), request.password());
            authenticationManager.authenticate(authenticationToken);
        } catch (AuthenticationException exception) {
            throw new UnauthorizedException("Invalid username or password");
        }

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));
        return createAuthResponse(user);
    }

    public RefreshResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        boolean expired = refreshToken.getExpiryDate().isBefore(Instant.now());
        if (refreshToken.isRevoked() || expired) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }

        String accessToken = jwtTokenProvider.createAccessToken(refreshToken.getUser());
        return new RefreshResponse(accessToken, "Bearer");
    }

    public void logout(LogoutRequest request, String accessToken) {
        tokenBlacklistService.blacklist(accessToken);

        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            return;
        }

        String currentUsername = SecurityUtils.currentUsername();
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .filter(token -> token.getUser().getUsername().equals(currentUsername))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        refreshTokenRepository.delete(refreshToken);
    }

    public void changePassword(ChangePasswordRequest request) {
        String currentUsername = SecurityUtils.currentUsername();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        String encodedPassword = passwordEncoder.encode(request.newPassword());
        user.setPassword(encodedPassword);
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        otpService.issueOtp(request.email());
    }

    public void resetPassword(ResetPasswordRequest request) {
        otpService.verifyAndReset(request.email(), request.otp(), request.newPassword());
    }

    private AuthResponse createAuthResponse(User user) {
        String refreshTokenValue = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plusMillis(jwtProperties.refreshTokenExpirationMs());
        RefreshToken refreshToken = RefreshToken.builder().token(refreshTokenValue).user(user).expiryDate(expiryDate)
                .build();
        refreshTokenRepository.save(refreshToken);

        String accessToken = jwtTokenProvider.createAccessToken(user);
        return new AuthResponse(accessToken, refreshTokenValue, "Bearer");
    }
}
