package com.project.modules.timeslot.dto.response;

import java.math.BigDecimal;
import java.time.LocalTime;

public record TimeSlotResponse(Long id, Long courtId, LocalTime startTime, LocalTime endTime, BigDecimal price,
        boolean active) {
}
