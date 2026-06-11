package com.project.modules.user.dto.request;

import jakarta.validation.constraints.*;

import com.project.common.enums.*;

public record UpdateUserRequest(@NotBlank String fullName, @Email @NotBlank String email, String phone,
        Boolean isActive, RoleName role) {
}
