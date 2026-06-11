package com.project.modules.timeslot.dto.request;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.*;

public record UpdateTimeSlotRequest(
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        @NotNull @Positive Integer price,
        Boolean active) {
}
