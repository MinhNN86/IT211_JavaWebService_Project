package com.project.modules.timeslot.dto.request;

import java.time.LocalTime;

import jakarta.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonFormat;

public record CreateTimeSlotRequest(
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime endTime) {
}
