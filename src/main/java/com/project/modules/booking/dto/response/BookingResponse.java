package com.project.modules.booking.dto.response;

import java.time.*;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.common.enums.BookingStatus;
import com.project.modules.timeslot.dto.response.TimeSlotResponse;

public record BookingResponse(
                Long id,
                UUID customerId,
                String customerUsername,
                Long courtId,
                String courtName,
                List<TimeSlotResponse> timeSlots,
                LocalDate bookingDate,
                BookingStatus status,
                String note,
                JsonNode priceSnapshot,
                LocalDateTime createdAt) {
}
