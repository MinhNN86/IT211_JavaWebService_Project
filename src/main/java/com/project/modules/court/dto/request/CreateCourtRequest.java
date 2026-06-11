package com.project.modules.court.dto.request;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.*;

public record CreateCourtRequest(
                @NotBlank String name,
                String description,
                @NotBlank String address,
                Set<UUID> managerIds) {
}
