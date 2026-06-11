package com.project.modules.timeslot.service.impl;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.common.exception.BadRequestException;
import com.project.common.exception.ConflictException;
import com.project.common.exception.NotFoundException;
import com.project.modules.court.entity.Court;
import com.project.modules.court.repository.CourtRepository;
import com.project.modules.court.service.CourtAccessService;
import com.project.modules.timeslot.dto.request.BulkCreateTimeSlotRequest;
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
        Court court = requireCourt(courtId);
        validateTimeRange(request.startTime(), request.endTime());

        if (timeSlotRepository.existsByCourtIdAndStartTimeAndEndTime(
                courtId, request.startTime(), request.endTime())) {
            throw new ConflictException("Time slot already exists for this court");
        }

        TimeSlot timeSlot = buildTimeSlot(court, request.startTime(), request.endTime(), request.price());
        TimeSlot savedTimeSlot = timeSlotRepository.save(timeSlot);

        return toResponse(savedTimeSlot);
    }

    @Override
    public List<TimeSlotResponse> createBulk(Long courtId, BulkCreateTimeSlotRequest request) {
        courtAccessService.requireCanManage(courtId);
        Court court = requireCourt(courtId);
        validateTimeRange(request.startTime(), request.endTime());
        validateDuration(request.startTime(), request.endTime(), request.durationMinutes());

        List<TimeSlot> timeSlots = new ArrayList<>();
        for (LocalTime startTime = request.startTime(); startTime.isBefore(request.endTime());
                startTime = startTime.plusMinutes(request.durationMinutes())) {
            LocalTime endTime = startTime.plusMinutes(request.durationMinutes());

            if (timeSlotRepository.existsByCourtIdAndStartTimeAndEndTime(courtId, startTime, endTime)) {
                throw new ConflictException(
                        "Time slot already exists for this court: " + startTime + " - " + endTime);
            }

            timeSlots.add(buildTimeSlot(court, startTime, endTime, request.price()));
        }

        return timeSlotRepository.saveAll(timeSlots).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public TimeSlotResponse update(Long courtId, Long id, UpdateTimeSlotRequest request) {
        courtAccessService.requireCanManage(courtId);
        validateTimeRange(request.startTime(), request.endTime());
        TimeSlot timeSlot = findTimeSlot(courtId, id);

        if (timeSlotRepository.existsByCourtIdAndStartTimeAndEndTimeAndIdNot(
                courtId, request.startTime(), request.endTime(), id)) {
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

    private Court requireCourt(Long courtId) {
        return courtRepository.findById(courtId)
                .orElseThrow(() -> new NotFoundException("Court not found"));
    }

    private TimeSlot findTimeSlot(Long courtId, Long timeSlotId) {
        return timeSlotRepository.findByIdAndCourtId(timeSlotId, courtId)
                .orElseThrow(() -> new NotFoundException("Time slot not found"));
    }

    private TimeSlot buildTimeSlot(Court court, LocalTime startTime, LocalTime endTime, BigDecimal price) {
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
