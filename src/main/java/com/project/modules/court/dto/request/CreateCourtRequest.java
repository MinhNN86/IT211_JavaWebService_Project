package com.project.modules.court.dto.request;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.*;

public record CreateCourtRequest(@NotBlank String name, String description, @NotBlank String address,
        @NotNull @Positive BigDecimal pricePerHour, Set<UUID> managerIds) {
}
