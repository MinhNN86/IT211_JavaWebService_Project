package com.project.modules.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @Email @NotBlank String email,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "OTP must be 6 digits") String otp,
        @NotBlank @Size(min = 6, message = "Password must be at least 6 characters") String newPassword) {
}
