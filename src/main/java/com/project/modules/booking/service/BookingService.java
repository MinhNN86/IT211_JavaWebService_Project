package com.project.modules.booking.service;

import org.springframework.data.domain.Pageable;

import com.project.common.response.PageResponse;
import com.project.modules.booking.dto.request.*;
import com.project.modules.booking.dto.response.BookingResponse;

public interface BookingService {
    BookingResponse create(CreateBookingRequest r);

    PageResponse<BookingResponse> myBookings(Pageable p);

    PageResponse<BookingResponse> all(Pageable p);

    BookingResponse findById(Long id);

    BookingResponse updateStatus(Long id, UpdateBookingStatusRequest r);
}
