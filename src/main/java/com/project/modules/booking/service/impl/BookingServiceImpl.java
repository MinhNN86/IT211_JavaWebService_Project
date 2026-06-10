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
import com.project.modules.court.service.CourtAccessService;
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
    private final CourtAccessService courtAccess;
    private final TimeSlotRepository slots;
    private final BookingMapper mapper;
    public BookingResponse create(CreateBookingRequest r) {
        var court = courts.findByIdForUpdate(r.courtId()).orElseThrow(() -> new NotFoundException("Court not found"));
        if (r.timeSlotIds() == null || r.timeSlotIds().isEmpty())
            throw new BadRequestException("At least one time slot is required");
        var slotIds = new LinkedHashSet<>(r.timeSlotIds());
        if (slotIds.size() != r.timeSlotIds().size())
            throw new BadRequestException("Time slot IDs must be unique");
        var selectedSlots = slots.findAllById(slotIds);
        if (selectedSlots.size() != slotIds.size())
            throw new NotFoundException("One or more time slots not found");
        if (court.getStatus() != CourtStatus.ACTIVE || selectedSlots.stream().anyMatch(slot -> !slot.isActive()))
            throw new BadRequestException("Court or time slot is not available");
        if (bookings.existsBlockingBooking(r.courtId(), r.bookingDate(), slotIds, BLOCKING))
            throw new ConflictException("Court is already booked for one or more selected time slots");
        var user = users.findByUsername(SecurityUtils.currentUsername())
                .orElseThrow(() -> new NotFoundException("User not found"));
        return mapper.toResponse(bookings.save(Booking.builder().customer(user).court(court)
                .timeSlots(new LinkedHashSet<>(selectedSlots)).bookingDate(r.bookingDate()).note(r.note()).build()));
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> myBookings(Pageable p) {
        var page = bookings.findByCustomerUsername(SecurityUtils.currentUsername(), p);
        return PageResponse.from(page, page.stream().map(mapper::toResponse).toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> all(Pageable p) {
        var page = SecurityUtils.hasRole("ADMIN")
                ? bookings.findAll(p)
                : bookings.findManagedBookings(SecurityUtils.currentUsername(), p);
        return PageResponse.from(page, page.stream().map(mapper::toResponse).toList());
    }

    @Transactional(readOnly = true)
    public BookingResponse findById(Long id) {
        var booking = get(id);
        courtAccess.requireCanManage(booking.getCourt().getId());
        return mapper.toResponse(booking);
    }

    public BookingResponse updateStatus(Long id, UpdateBookingStatusRequest r) {
        var b = get(id);
        courtAccess.requireCanManage(b.getCourt().getId());
        if (!TRANSITIONS.getOrDefault(b.getStatus(), Set.of()).contains(r.status()))
            throw new BadRequestException("Invalid booking status transition");
        b.setStatus(r.status());
        return mapper.toResponse(b);
    }

    private Booking get(Long id) {
        return bookings.findById(id).orElseThrow(() -> new NotFoundException("Booking not found"));
    }
}
