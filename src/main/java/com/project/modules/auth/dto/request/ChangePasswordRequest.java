package com.project.modules.auth.dto.request;

import jakarta.validation.constraints.*;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @Size(min = 6) String newPassword) {
}
