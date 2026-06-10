package com.project.modules.court.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;

import com.project.common.enums.CourtStatus;

public record UpdateCourtRequest(@NotBlank String name, String description, @NotBlank String address,
        @NotNull @Positive BigDecimal pricePerHour, CourtStatus status) {
}
