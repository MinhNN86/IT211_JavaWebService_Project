package com.project.modules.user.dto.request;

import jakarta.validation.constraints.*;

public record UpdateProfileRequest(
        @NotBlank String fullName,
        @Email @NotBlank String email,
        String phone) {
}
