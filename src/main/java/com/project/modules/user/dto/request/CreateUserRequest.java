package com.project.modules.user.dto.request;

import jakarta.validation.constraints.*;

import com.project.common.enums.RoleName;

public record CreateUserRequest(@NotBlank String fullName, @NotBlank String username, @Email @NotBlank String email,
        @Size(min = 6) String password, String phone, RoleName role) {
}
