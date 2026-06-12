package com.project.modules.booking.service.impl;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.common.enums.BookingStatus;
import com.project.common.enums.CourtStatus;
import com.project.common.exception.BadRequestException;
import com.project.common.exception.ConflictException;
import com.project.common.exception.NotFoundException;
import com.project.common.response.PageResponse;
import com.project.common.util.SecurityUtils;
import com.project.modules.booking.dto.request.CreateBookingRequest;
import com.project.modules.booking.dto.request.UpdateBookingStatusRequest;
import com.project.modules.booking.dto.response.BookingResponse;
import com.project.modules.booking.entity.Booking;
import com.project.modules.booking.mapper.BookingMapper;
import com.project.modules.booking.repository.BookingRepository;
import com.project.modules.booking.service.BookingService;
import com.project.modules.court.entity.Court;
import com.project.modules.court.repository.CourtRepository;
import com.project.modules.court.service.CourtAccessService;
import com.project.modules.timeslot.entity.TimeSlot;
import com.project.modules.timeslot.repository.TimeSlotRepository;
import com.project.modules.user.entity.User;
import com.project.modules.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {
    private static final List<BookingStatus> BLOCKING_STATUSES = List.of(BookingStatus.PENDING,
            BookingStatus.CONFIRMED);
    private static final Map<BookingStatus, Set<BookingStatus>> ALLOWED_STATUS_TRANSITIONS = Map.of(
            BookingStatus.PENDING, Set.of(BookingStatus.CONFIRMED, BookingStatus.REJECTED), BookingStatus.CONFIRMED,
            Set.of(BookingStatus.COMPLETED, BookingStatus.CANCELLED));

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final CourtRepository courtRepository;
    private final CourtAccessService courtAccessService;
    private final TimeSlotRepository timeSlotRepository;
    private final BookingMapper bookingMapper;
    private final ObjectMapper objectMapper;

    public BookingResponse create(CreateBookingRequest request) {
        Court court = courtRepository.findByIdForUpdate(request.courtId())
                .orElseThrow(() -> new NotFoundException("Court not found"));

        if (request.timeSlotIds() == null || request.timeSlotIds().isEmpty()) {
            throw new BadRequestException("At least one time slot is required");
        }

        Set<Long> timeSlotIds = new LinkedHashSet<>(request.timeSlotIds());
        if (timeSlotIds.size() != request.timeSlotIds().size()) {
            throw new BadRequestException("Time slot IDs must be unique");
        }

        List<TimeSlot> selectedTimeSlots = timeSlotRepository.findAllById(timeSlotIds);
        if (selectedTimeSlots.size() != timeSlotIds.size()) {
            throw new NotFoundException("One or more time slots not found");
        }
        if (selectedTimeSlots.stream().anyMatch(timeSlot -> !timeSlot.getCourt().getId().equals(request.courtId()))) {
            throw new BadRequestException("All time slots must belong to the selected court");
        }
        if (court.getStatus() != CourtStatus.ACTIVE
                || selectedTimeSlots.stream().anyMatch(timeSlot -> !timeSlot.isActive())) {
            throw new BadRequestException("Court or time slot is not available");
        }
        if (bookingRepository.existsBlockingBooking(request.courtId(), request.bookingDate(), timeSlotIds,
                BLOCKING_STATUSES)) {
            throw new ConflictException("Court is already booked for one or more selected time slots");
        }

        String currentUsername = SecurityUtils.currentUsername();
        User customer = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Booking booking = Booking.builder().customer(customer).court(court)
                .timeSlots(new LinkedHashSet<>(selectedTimeSlots)).bookingDate(request.bookingDate())
                .note(request.note()).priceSnapshot(createPriceSnapshot(selectedTimeSlots))
                .build();
        Booking savedBooking = bookingRepository.save(booking);
        return bookingMapper.toResponse(savedBooking);
    }

    private ObjectNode createPriceSnapshot(List<TimeSlot> timeSlots) {
        ArrayNode slotSnapshots = objectMapper.createArrayNode();
        long totalPrice = 0;

        for (TimeSlot timeSlot : timeSlots.stream().sorted(Comparator.comparing(TimeSlot::getStartTime)).toList()) {
            ObjectNode slotSnapshot = objectMapper.createObjectNode();
            slotSnapshot.put("timeSlotId", timeSlot.getId());
            slotSnapshot.put("startTime", timeSlot.getStartTime().toString());
            slotSnapshot.put("endTime", timeSlot.getEndTime().toString());
            slotSnapshot.put("price", timeSlot.getPrice());
            slotSnapshots.add(slotSnapshot);
            totalPrice += timeSlot.getPrice();
        }

        ObjectNode snapshot = objectMapper.createObjectNode();
        snapshot.set("timeSlots", slotSnapshots);
        snapshot.put("totalPrice", totalPrice);
        return snapshot;
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> myBookings(Pageable pageable) {
        String currentUsername = SecurityUtils.currentUsername();
        Page<Booking> bookingPage = bookingRepository.findByCustomerUsername(currentUsername, pageable);
        List<BookingResponse> bookingResponses = bookingPage.stream().map(bookingMapper::toResponse).toList();
        return PageResponse.from(bookingPage, bookingResponses);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> all(Pageable pageable) {
        Page<Booking> bookingPage;
        if (SecurityUtils.hasRole("ADMIN")) {
            bookingPage = bookingRepository.findAll(pageable);
        } else {
            String currentUsername = SecurityUtils.currentUsername();
            bookingPage = bookingRepository.findManagedBookings(currentUsername, pageable);
        }

        List<BookingResponse> bookingResponses = bookingPage.stream().map(bookingMapper::toResponse).toList();
        return PageResponse.from(bookingPage, bookingResponses);
    }

    @Transactional(readOnly = true)
    public BookingResponse findById(Long id) {
        Booking booking = getBooking(id);
        courtAccessService.requireCanManage(booking.getCourt().getId());
        return bookingMapper.toResponse(booking);
    }

    public BookingResponse updateStatus(Long id, UpdateBookingStatusRequest request) {
        Booking booking = getBooking(id);
        courtAccessService.requireCanManage(booking.getCourt().getId());

        Set<BookingStatus> allowedStatuses = ALLOWED_STATUS_TRANSITIONS.getOrDefault(booking.getStatus(), Set.of());
        if (!allowedStatuses.contains(request.status())) {
            throw new BadRequestException("Invalid booking status transition");
        }

        booking.setStatus(request.status());
        return bookingMapper.toResponse(booking);
    }

    private Booking getBooking(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> new NotFoundException("Booking not found"));
    }
}
