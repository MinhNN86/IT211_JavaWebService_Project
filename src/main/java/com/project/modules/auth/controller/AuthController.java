package com.project.modules.auth.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.project.common.exception.UnauthorizedException;
import com.project.common.response.ApiResponse;
import com.project.modules.auth.dto.request.*;
import com.project.modules.auth.dto.response.AuthResponse;
import com.project.modules.auth.dto.response.RefreshResponse;
import com.project.modules.auth.service.AuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService service;

    @PostMapping("/register")
    ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.success("Registered successfully", service.register(request)));
    }

    @PostMapping("/login")
    ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("Login successfully", service.login(request));
    }

    @PostMapping("/refresh")
    ApiResponse<RefreshResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success("Token refreshed", service.refresh(request));
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(@Valid @RequestBody(required = false) LogoutRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        service.logout(request, extractAccessToken(authorizationHeader));
        return ApiResponse.success("Logout successfully", null);
    }

    @PostMapping("/change-password")
    ApiResponse<Void> change(@Valid @RequestBody ChangePasswordRequest request) {
        service.changePassword(request);
        return ApiResponse.success("Password changed", null);
    }

    @PostMapping("/forgot-password")
    ApiResponse<Void> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        service.forgotPassword(request);
        return ApiResponse.success("If the account exists, a reset request was accepted", null);
    }

    private String extractAccessToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException("Missing access token");
        }
        return authorizationHeader.substring(BEARER_PREFIX.length());
    }
}
