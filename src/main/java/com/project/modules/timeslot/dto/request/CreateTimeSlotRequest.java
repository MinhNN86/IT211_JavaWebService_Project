package com.project.modules.timeslot.dto.request;

import java.math.BigDecimal;
import java.time.LocalTime;

import jakarta.validation.constraints.*;

public record CreateTimeSlotRequest(@NotNull LocalTime startTime, @NotNull LocalTime endTime,
        @NotNull @Positive BigDecimal price) {
}
