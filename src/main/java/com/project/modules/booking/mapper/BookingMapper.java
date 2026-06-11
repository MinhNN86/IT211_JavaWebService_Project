package com.project.modules.booking.mapper;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.project.modules.booking.dto.response.BookingResponse;
import com.project.modules.booking.entity.Booking;
import com.project.modules.timeslot.dto.response.TimeSlotResponse;
import com.project.modules.timeslot.entity.TimeSlot;

@Component
public class BookingMapper {
    public BookingResponse toResponse(Booking booking) {
        List<TimeSlotResponse> timeSlotResponses = booking.getTimeSlots().stream()
                .sorted(Comparator.comparing(TimeSlot::getStartTime)).map(this::toTimeSlotResponse).toList();

        return new BookingResponse(booking.getId(), booking.getCustomer().getId(), booking.getCustomer().getUsername(),
                booking.getCourt().getId(), booking.getCourt().getName(), timeSlotResponses, booking.getBookingDate(),
                booking.getStatus(), booking.getNote(), booking.getCreatedAt());
    }

    private TimeSlotResponse toTimeSlotResponse(TimeSlot timeSlot) {
        return new TimeSlotResponse(timeSlot.getId(), timeSlot.getCourt().getId(), timeSlot.getStartTime(),
                timeSlot.getEndTime(), timeSlot.getPrice(), timeSlot.isActive());
    }
}
