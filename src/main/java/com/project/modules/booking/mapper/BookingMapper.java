package com.project.modules.booking.mapper;

import org.springframework.stereotype.Component;

import com.project.modules.booking.dto.response.BookingResponse;
import com.project.modules.booking.entity.Booking;

@Component
public class BookingMapper {
    public BookingResponse toResponse(Booking b) {
        return new BookingResponse(b.getId(), b.getCustomer().getId(), b.getCustomer().getUsername(),
                b.getCourt().getId(), b.getCourt().getName(), b.getTimeSlot().getId(), b.getTimeSlot().getStartTime(),
                b.getTimeSlot().getEndTime(), b.getBookingDate(), b.getStatus(), b.getNote(), b.getCreatedAt());
    }
}
