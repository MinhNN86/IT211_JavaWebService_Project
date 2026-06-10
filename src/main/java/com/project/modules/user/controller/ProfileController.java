package com.project.modules.user.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.project.common.response.ApiResponse;
import com.project.modules.user.dto.request.UpdateProfileRequest;
import com.project.modules.user.dto.response.UserResponse;
import com.project.modules.user.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final UserService service;
    @GetMapping
    ApiResponse<UserResponse> get() {
        return ApiResponse.success("Profile retrieved", service.profile());
    }

    @PutMapping
    ApiResponse<UserResponse> update(@Valid @RequestBody UpdateProfileRequest r) {
        return ApiResponse.success("Profile updated", service.updateProfile(r));
    }
}
