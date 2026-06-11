package com.project.modules.timeslot.dto.response;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record TimeSlotResponse(
        Long id,
        Long courtId,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        Integer price,
        boolean active) {
}
