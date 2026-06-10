package com.project.modules.booking.dto.response;

import java.time.*;
import java.util.UUID;

import com.project.common.enums.BookingStatus;

public record BookingResponse(Long id, UUID customerId, String customerUsername, Long courtId, String courtName,
        Long timeSlotId, LocalTime startTime, LocalTime endTime, LocalDate bookingDate, BookingStatus status,
        String note, LocalDateTime createdAt) {
}
