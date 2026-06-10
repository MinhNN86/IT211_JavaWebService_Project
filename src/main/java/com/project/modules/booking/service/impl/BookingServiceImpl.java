package com.project.modules.booking.service.impl;

import java.util.*;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.common.enums.*;
import com.project.common.exception.*;
import com.project.common.response.PageResponse;
import com.project.common.util.SecurityUtils;
import com.project.modules.booking.dto.request.*;
import com.project.modules.booking.dto.response.BookingResponse;
import com.project.modules.booking.entity.Booking;
import com.project.modules.booking.mapper.BookingMapper;
import com.project.modules.booking.repository.BookingRepository;
import com.project.modules.booking.service.BookingService;
import com.project.modules.court.repository.CourtRepository;
import com.project.modules.timeslot.repository.TimeSlotRepository;
import com.project.modules.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {
    private static final List<BookingStatus> BLOCKING = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);
    private static final Map<BookingStatus, Set<BookingStatus>> TRANSITIONS = Map.of(BookingStatus.PENDING,
            Set.of(BookingStatus.CONFIRMED, BookingStatus.REJECTED), BookingStatus.CONFIRMED,
            Set.of(BookingStatus.COMPLETED, BookingStatus.CANCELLED));
    private final BookingRepository bookings;
    private final UserRepository users;
    private final CourtRepository courts;
    private final TimeSlotRepository slots;
    private final BookingMapper mapper;
    public BookingResponse create(CreateBookingRequest r) {
        var court = courts.findByIdForUpdate(r.courtId()).orElseThrow(() -> new NotFoundException("Court not found"));
        if (bookings.existsByCourtIdAndBookingDateAndTimeSlotIdAndStatusIn(r.courtId(), r.bookingDate(), r.timeSlotId(),
                BLOCKING))
            throw new ConflictException("Court is already booked for this time slot");
        var user = users.findByUsername(SecurityUtils.currentUsername())
                .orElseThrow(() -> new NotFoundException("User not found"));
        var slot = slots.findById(r.timeSlotId()).orElseThrow(() -> new NotFoundException("Time slot not found"));
        if (court.getStatus() != CourtStatus.ACTIVE || !slot.isActive())
            throw new BadRequestException("Court or time slot is not available");
        return mapper.toResponse(bookings.save(Booking.builder().customer(user).court(court).timeSlot(slot)
                .bookingDate(r.bookingDate()).note(r.note()).build()));
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> myBookings(Pageable p) {
        var page = bookings.findByCustomerUsername(SecurityUtils.currentUsername(), p);
        return PageResponse.from(page, page.stream().map(mapper::toResponse).toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> all(Pageable p) {
        var page = bookings.findAll(p);
        return PageResponse.from(page, page.stream().map(mapper::toResponse).toList());
    }

    @Transactional(readOnly = true)
    public BookingResponse findById(Long id) {
        return mapper.toResponse(get(id));
    }

    public BookingResponse updateStatus(Long id, UpdateBookingStatusRequest r) {
        var b = get(id);
        if (!TRANSITIONS.getOrDefault(b.getStatus(), Set.of()).contains(r.status()))
            throw new BadRequestException("Invalid booking status transition");
        b.setStatus(r.status());
        return mapper.toResponse(b);
    }

    private Booking get(Long id) {
        return bookings.findById(id).orElseThrow(() -> new NotFoundException("Booking not found"));
    }
}
