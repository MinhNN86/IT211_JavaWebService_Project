package com.project.modules.booking.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import com.project.common.response.*;
import com.project.modules.booking.dto.request.UpdateBookingStatusRequest;
import com.project.modules.booking.dto.response.BookingResponse;
import com.project.modules.booking.service.BookingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/manager/bookings")
@RequiredArgsConstructor
public class ManagerBookingController {
    private final BookingService service;
    @GetMapping
    ApiResponse<PageResponse<BookingResponse>> all(Pageable p) {
        return ApiResponse.success("Bookings retrieved", service.all(p));
    }

    @GetMapping("/{id}")
    ApiResponse<BookingResponse> one(@PathVariable Long id) {
        return ApiResponse.success("Booking retrieved", service.findById(id));
    }

    @PutMapping("/{id}/status")
    ApiResponse<BookingResponse> status(@PathVariable Long id, @Valid @RequestBody UpdateBookingStatusRequest r) {
        return ApiResponse.success("Booking status updated", service.updateStatus(id, r));
    }
}
