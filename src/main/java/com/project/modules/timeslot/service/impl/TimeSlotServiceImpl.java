package com.project.modules.timeslot.service.impl;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.common.exception.BadRequestException;
import com.project.common.exception.ConflictException;
import com.project.common.exception.NotFoundException;
import com.project.modules.court.entity.Court;
import com.project.modules.court.repository.CourtRepository;
import com.project.modules.court.service.CourtAccessService;
import com.project.modules.timeslot.dto.request.BulkCreateTimeSlotRequest;
import com.project.modules.timeslot.dto.request.BulkUpdatePriceRequest;
import com.project.modules.timeslot.dto.request.CreateTimeSlotRequest;
import com.project.modules.timeslot.dto.request.UpdateTimeSlotRequest;
import com.project.modules.timeslot.dto.response.TimeSlotResponse;
import com.project.modules.timeslot.entity.TimeSlot;
import com.project.modules.timeslot.repository.TimeSlotRepository;
import com.project.modules.timeslot.service.TimeSlotService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TimeSlotServiceImpl implements TimeSlotService {
    private final TimeSlotRepository timeSlotRepository;
    private final CourtRepository courtRepository;
    private final CourtAccessService courtAccessService;

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotResponse> findByCourt(Long courtId) {
        requireCourt(courtId);

        return timeSlotRepository.findByCourtIdOrderByStartTime(courtId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public TimeSlotResponse create(Long courtId, CreateTimeSlotRequest request) {
        courtAccessService.requireCanManage(courtId);
        Court court = requireCourtForUpdate(courtId);
        validateTimeRange(request.startTime(), request.endTime());
        validateTimeAlignment(request.startTime(), request.endTime());
        validateSlotDuration(request.startTime(), request.endTime());

        requireNoActiveOverlap(courtId, request.startTime(), request.endTime());

        TimeSlot timeSlot = timeSlotRepository
                .findByCourtIdAndStartTimeAndEndTime(courtId, request.startTime(), request.endTime())
                .map(existing -> reactivate(existing, 0))
                .orElseGet(() -> buildTimeSlot(court, request.startTime(), request.endTime(), 0));
        TimeSlot savedTimeSlot = timeSlotRepository.save(timeSlot);

        return toResponse(savedTimeSlot);
    }

    @Override
    public List<TimeSlotResponse> createBulk(Long courtId, BulkCreateTimeSlotRequest request) {
        courtAccessService.requireCanManage(courtId);
        Court court = requireCourtForUpdate(courtId);
        validateTimeRange(request.startTime(), request.endTime());
        validateTimeAlignment(request.startTime(), request.endTime());
        validateDuration(request.startTime(), request.endTime(), request.durationMinutes());

        List<TimeSlot> timeSlots = new ArrayList<>();
        for (LocalTime startTime = request.startTime(); startTime
                .isBefore(request.endTime()); startTime = startTime.plusMinutes(request.durationMinutes())) {
            LocalTime endTime = startTime.plusMinutes(request.durationMinutes());

            requireNoActiveOverlap(courtId, startTime, endTime);

            TimeSlot timeSlot = timeSlotRepository.findByCourtIdAndStartTimeAndEndTime(courtId, startTime, endTime)
                    .orElse(null);
            timeSlot = timeSlot == null
                    ? buildTimeSlot(court, startTime, endTime, 0)
                    : reactivate(timeSlot, 0);
            timeSlots.add(timeSlot);
        }

        return timeSlotRepository.saveAll(timeSlots).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<TimeSlotResponse> updatePrices(BulkUpdatePriceRequest request) {
        Set<Long> seen = new HashSet<>();
        for (BulkUpdatePriceRequest.PriceEntry entry : request.entries()) {
            for (Long id : entry.timeSlotIds()) {
                if (!seen.add(id)) {
                    throw new BadRequestException("Duplicate time slot ID: " + id);
                }
            }
        }

        List<TimeSlot> updated = new ArrayList<>();
        for (BulkUpdatePriceRequest.PriceEntry entry : request.entries()) {
            for (Long id : entry.timeSlotIds()) {
                TimeSlot timeSlot = timeSlotRepository.findById(id)
                        .orElseThrow(() -> new NotFoundException("Time slot not found: " + id));
                courtAccessService.requireCanManage(timeSlot.getCourt().getId());
                timeSlot.setPrice(entry.price());
                updated.add(timeSlot);
            }
        }

        return updated.stream().map(this::toResponse).toList();
    }

    @Override
    public TimeSlotResponse update(Long courtId, Long id, UpdateTimeSlotRequest request) {
        courtAccessService.requireCanManage(courtId);
        requireCourtForUpdate(courtId);
        validateTimeRange(request.startTime(), request.endTime());
        validateTimeAlignment(request.startTime(), request.endTime());
        validateSlotDuration(request.startTime(), request.endTime());
        TimeSlot timeSlot = findTimeSlot(courtId, id);

        boolean willBeActive = request.active() == null ? timeSlot.isActive() : request.active();
        if (willBeActive
                && timeSlotRepository.existsByCourtIdAndActiveTrueAndIdNotAndStartTimeLessThanAndEndTimeGreaterThan(
                        courtId, id, request.endTime(), request.startTime())) {
            throw new ConflictException("Time slot overlaps an active time slot for this court");
        }
        if (timeSlotRepository.existsByCourtIdAndIdNotAndStartTimeAndEndTime(
                courtId, id, request.startTime(), request.endTime())) {
            throw new ConflictException("Time slot already exists for this court");
        }

        timeSlot.setStartTime(request.startTime());
        timeSlot.setEndTime(request.endTime());
        timeSlot.setPrice(request.price());
        if (request.active() != null) {
            timeSlot.setActive(request.active());
        }

        return toResponse(timeSlot);
    }

    @Override
    public void delete(Long courtId, Long id) {
        courtAccessService.requireCanManage(courtId);
        requireCourtForUpdate(courtId);
        TimeSlot timeSlot = findTimeSlot(courtId, id);
        timeSlot.setActive(false);
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new BadRequestException("Start time must be before end time");
        }
    }

    private void validateDuration(LocalTime startTime, LocalTime endTime, int durationMinutes) {
        if (durationMinutes != 30 && durationMinutes != 60) {
            throw new BadRequestException("Duration must be 30 or 60 minutes");
        }

        long rangeMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
        boolean hasWholeMinuteRange = startTime.plusMinutes(rangeMinutes).equals(endTime);
        boolean isRangeDivisibleByDuration = rangeMinutes % durationMinutes == 0;
        if (!hasWholeMinuteRange || !isRangeDivisibleByDuration) {
            throw new BadRequestException("Time range must be divisible by duration");
        }
    }

    private void validateSlotDuration(LocalTime startTime, LocalTime endTime) {
        long durationMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
        boolean hasWholeMinuteDuration = startTime.plusMinutes(durationMinutes).equals(endTime);
        if (!hasWholeMinuteDuration || (durationMinutes != 30 && durationMinutes != 60)) {
            throw new BadRequestException("Time slot duration must be 30 or 60 minutes");
        }
    }

    private void validateTimeAlignment(LocalTime startTime, LocalTime endTime) {
        if (!isAlignedToThirtyMinutes(startTime) || !isAlignedToThirtyMinutes(endTime)) {
            throw new BadRequestException("Start and end times must align to 30-minute intervals");
        }
    }

    private boolean isAlignedToThirtyMinutes(LocalTime time) {
        return time.getMinute() % 30 == 0 && time.getSecond() == 0 && time.getNano() == 0;
    }

    private void requireNoActiveOverlap(Long courtId, LocalTime startTime, LocalTime endTime) {
        if (timeSlotRepository.existsByCourtIdAndActiveTrueAndStartTimeLessThanAndEndTimeGreaterThan(
                courtId, endTime, startTime)) {
            throw new ConflictException("Time slot overlaps an active time slot for this court");
        }
    }

    private TimeSlot reactivate(TimeSlot timeSlot, Integer price) {
        timeSlot.setPrice(price);
        timeSlot.setActive(true);
        return timeSlot;
    }

    private Court requireCourt(Long courtId) {
        return courtRepository.findById(courtId)
                .orElseThrow(() -> new NotFoundException("Court not found"));
    }

    private Court requireCourtForUpdate(Long courtId) {
        return courtRepository.findByIdForUpdate(courtId)
                .orElseThrow(() -> new NotFoundException("Court not found"));
    }

    private TimeSlot findTimeSlot(Long courtId, Long timeSlotId) {
        return timeSlotRepository.findByIdAndCourtId(timeSlotId, courtId)
                .orElseThrow(() -> new NotFoundException("Time slot not found"));
    }

    private TimeSlot buildTimeSlot(Court court, LocalTime startTime, LocalTime endTime, Integer price) {
        return TimeSlot.builder()
                .court(court)
                .startTime(startTime)
                .endTime(endTime)
                .price(price)
                .build();
    }

    private TimeSlotResponse toResponse(TimeSlot timeSlot) {
        return new TimeSlotResponse(
                timeSlot.getId(),
                timeSlot.getCourt().getId(),
                timeSlot.getStartTime(),
                timeSlot.getEndTime(),
                timeSlot.getPrice(),
                timeSlot.isActive());
    }
}
