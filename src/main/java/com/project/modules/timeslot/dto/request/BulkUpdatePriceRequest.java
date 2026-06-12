package com.project.modules.timeslot.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record BulkUpdatePriceRequest(@NotEmpty List<@Valid PriceEntry> entries) {
    public record PriceEntry(
            @NotEmpty List<@NotNull Long> timeSlotIds,
            @NotNull @Positive Integer price) {
    }
}
