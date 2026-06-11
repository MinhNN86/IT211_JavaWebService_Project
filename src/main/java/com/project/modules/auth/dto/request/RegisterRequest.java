package com.project.modules.auth.dto.request;

import jakarta.validation.constraints.*;

public record RegisterRequest(
                @NotBlank String fullName,
                @NotBlank String username,
                @Email @NotBlank String email,
                @Size(min = 6) String password,
                String phone) {
}
