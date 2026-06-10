package com.project.modules.booking.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import com.project.common.response.*;
import com.project.modules.booking.dto.request.CreateBookingRequest;
import com.project.modules.booking.dto.response.BookingResponse;
import com.project.modules.booking.service.BookingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/customer/bookings")
@RequiredArgsConstructor
public class CustomerBookingController {
    private final BookingService service;
    @PostMapping
    ResponseEntity<ApiResponse<BookingResponse>> create(@Valid @RequestBody CreateBookingRequest r) {
        return ResponseEntity.status(201).body(ApiResponse.success("Booking created", service.create(r)));
    }

    @GetMapping
    ApiResponse<PageResponse<BookingResponse>> mine(Pageable p) {
        return ApiResponse.success("Bookings retrieved", service.myBookings(p));
    }
}
