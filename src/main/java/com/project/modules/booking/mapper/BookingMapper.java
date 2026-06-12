package com.project.modules.booking.mapper;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.modules.booking.dto.response.BookingResponse;
import com.project.modules.booking.entity.Booking;
import com.project.modules.timeslot.dto.response.TimeSlotResponse;
import com.project.modules.timeslot.entity.TimeSlot;

@Component
public class BookingMapper {
    public BookingResponse toResponse(Booking booking) {
        Map<Long, Integer> bookedPrices = getBookedPrices(booking.getPriceSnapshot());
        List<TimeSlotResponse> timeSlotResponses = booking.getTimeSlots().stream()
                .sorted(Comparator.comparing(TimeSlot::getStartTime))
                .map(timeSlot -> toTimeSlotResponse(timeSlot, bookedPrices.get(timeSlot.getId()))).toList();

        return new BookingResponse(booking.getId(), booking.getCustomer().getId(), booking.getCustomer().getUsername(),
                booking.getCourt().getId(), booking.getCourt().getName(), timeSlotResponses, booking.getBookingDate(),
                booking.getStatus(), booking.getNote(), booking.getPriceSnapshot(), booking.getCreatedAt());
    }

    private TimeSlotResponse toTimeSlotResponse(TimeSlot timeSlot, Integer bookedPrice) {
        return new TimeSlotResponse(timeSlot.getId(), timeSlot.getCourt().getId(), timeSlot.getStartTime(),
                timeSlot.getEndTime(), bookedPrice != null ? bookedPrice : timeSlot.getPrice(), timeSlot.isActive());
    }

    private Map<Long, Integer> getBookedPrices(JsonNode priceSnapshot) {
        Map<Long, Integer> bookedPrices = new HashMap<>();
        if (priceSnapshot == null || !priceSnapshot.path("timeSlots").isArray()) {
            return bookedPrices;
        }
        for (JsonNode slot : priceSnapshot.path("timeSlots")) {
            if (slot.hasNonNull("timeSlotId") && slot.hasNonNull("price")) {
                bookedPrices.put(slot.get("timeSlotId").asLong(), slot.get("price").asInt());
            }
        }
        return bookedPrices;
    }
}
