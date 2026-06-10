package com.project.modules.court.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;

public record CreateCourtRequest(@NotBlank String name, String description, @NotBlank String address,
        @NotNull @Positive BigDecimal pricePerHour) {
}
