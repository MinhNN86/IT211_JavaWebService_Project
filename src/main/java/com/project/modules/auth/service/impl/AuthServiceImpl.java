package com.project.modules.auth.service.impl;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.common.enums.RoleName;
import com.project.common.exception.*;
import com.project.common.util.SecurityUtils;
import com.project.modules.auth.dto.request.*;
import com.project.modules.auth.dto.response.AuthResponse;
import com.project.modules.auth.entity.*;
import com.project.modules.auth.repository.*;
import com.project.modules.auth.service.AuthService;
import com.project.modules.user.entity.User;
import com.project.modules.user.repository.UserRepository;
import com.project.security.jwt.*;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwt;
    private final JwtProperties properties;
    public AuthResponse register(RegisterRequest r) {
        if (users.existsByUsername(r.username()) || users.existsByEmail(r.email()))
            throw new ConflictException("Username or email already exists");
        var user = users.save(User.builder().fullName(r.fullName()).username(r.username()).email(r.email())
                .password(encoder.encode(r.password())).phone(r.phone()).role(RoleName.CUSTOMER).build());
        return tokens(user);
    }

    public AuthResponse login(LoginRequest r) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(r.username(), r.password()));
        } catch (AuthenticationException ex) {
            throw new UnauthorizedException("Invalid username or password");
        }
        return tokens(users.findByUsername(r.username())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password")));
    }

    public AuthResponse refresh(RefreshTokenRequest r) {
        var token = refreshTokens.findByToken(r.refreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (token.isRevoked() || token.getExpiryDate().isBefore(Instant.now()))
            throw new UnauthorizedException("Refresh token expired or revoked");
        token.setRevoked(true);
        return tokens(token.getUser());
    }

    public void logout(LogoutRequest r) {
        var token = refreshTokens.findByToken(r.refreshToken())
                .filter(t -> t.getUser().getUsername().equals(SecurityUtils.currentUsername()))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        refreshTokens.delete(token);
    }

    public void changePassword(ChangePasswordRequest r) {
        var user = users.findByUsername(SecurityUtils.currentUsername())
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!encoder.matches(r.currentPassword(), user.getPassword()))
            throw new BadRequestException("Current password is incorrect");
        user.setPassword(encoder.encode(r.newPassword()));
    }

    public void forgotPassword(ForgotPasswordRequest r) {
        if (!users.existsByEmail(r.email()))
            throw new NotFoundException("Email not found");
    }

    private AuthResponse tokens(User user) {
        String refresh = UUID.randomUUID().toString();
        refreshTokens.save(RefreshToken.builder().token(refresh).user(user)
                .expiryDate(Instant.now().plusMillis(properties.refreshTokenExpirationMs())).build());
        return new AuthResponse(jwt.createAccessToken(user), refresh, "Bearer");
    }
}
