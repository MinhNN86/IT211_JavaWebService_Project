package com.project.modules.auth.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final AuthService service;
    @PostMapping("/register")
    ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest r) {
        return ResponseEntity.status(201).body(ApiResponse.success("Registered successfully", service.register(r)));
    }

    @PostMapping("/login")
    ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest r) {
        return ApiResponse.success("Login successfully", service.login(r));
    }

    @PostMapping("/refresh")
    ApiResponse<RefreshResponse> refresh(@Valid @RequestBody RefreshTokenRequest r) {
        return ApiResponse.success("Token refreshed", service.refresh(r));
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest r) {
        service.logout(r);
        return ApiResponse.success("Logout successfully", null);
    }

    @PostMapping("/change-password")
    ApiResponse<Void> change(@Valid @RequestBody ChangePasswordRequest r) {
        service.changePassword(r);
        return ApiResponse.success("Password changed", null);
    }

    @PostMapping("/forgot-password")
    ApiResponse<Void> forgot(@Valid @RequestBody ForgotPasswordRequest r) {
        service.forgotPassword(r);
        return ApiResponse.success("If the account exists, a reset request was accepted", null);
    }
}
