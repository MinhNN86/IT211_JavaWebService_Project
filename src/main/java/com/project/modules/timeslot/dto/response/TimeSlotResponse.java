package com.project.modules.timeslot.dto.response;

import java.math.BigDecimal;
import java.time.LocalTime;

public record TimeSlotResponse(Long id, LocalTime startTime, LocalTime endTime, BigDecimal price, boolean active) {
}
