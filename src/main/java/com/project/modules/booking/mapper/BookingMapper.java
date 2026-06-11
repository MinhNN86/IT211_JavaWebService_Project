package com.project.modules.booking.mapper;

import java.util.Comparator;

import org.springframework.stereotype.Component;

import com.project.modules.booking.dto.response.BookingResponse;
import com.project.modules.booking.entity.Booking;
import com.project.modules.timeslot.dto.response.TimeSlotResponse;

@Component
public class BookingMapper {
    public BookingResponse toResponse(Booking b) {
        var timeSlots = b.getTimeSlots().stream().sorted(Comparator.comparing(slot -> slot.getStartTime()))
                .map(slot -> new TimeSlotResponse(slot.getId(), slot.getCourt().getId(), slot.getStartTime(),
                        slot.getEndTime(), slot.getPrice(), slot.isActive()))
                .toList();
        return new BookingResponse(b.getId(), b.getCustomer().getId(), b.getCustomer().getUsername(),
                b.getCourt().getId(), b.getCourt().getName(), timeSlots, b.getBookingDate(), b.getStatus(), b.getNote(),
                b.getCreatedAt());
    }
}
