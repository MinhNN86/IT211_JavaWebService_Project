package com.project.modules.timeslot.dto.request;

import java.math.BigDecimal;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BulkCreateTimeSlotRequest(
                @NotNull LocalTime startTime,
                @NotNull LocalTime endTime,
                @NotNull @Positive Integer durationMinutes,
                @NotNull @Positive BigDecimal price) {
}
