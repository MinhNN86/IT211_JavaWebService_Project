package com.project.modules.timeslot.service.impl;

import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.common.exception.*;
import com.project.modules.court.entity.Court;
import com.project.modules.court.repository.CourtRepository;
import com.project.modules.court.service.CourtAccessService;
import com.project.modules.timeslot.dto.request.*;
import com.project.modules.timeslot.dto.response.TimeSlotResponse;
import com.project.modules.timeslot.entity.TimeSlot;
import com.project.modules.timeslot.repository.TimeSlotRepository;
import com.project.modules.timeslot.service.TimeSlotService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TimeSlotServiceImpl implements TimeSlotService {
    private final TimeSlotRepository repository;
    private final CourtRepository courts;
    private final CourtAccessService courtAccess;

    @Transactional(readOnly = true)
    public List<TimeSlotResponse> findByCourt(Long courtId) {
        requireCourt(courtId);
        return repository.findByCourtIdOrderByStartTime(courtId).stream().map(this::map).toList();
    }

    public TimeSlotResponse create(Long courtId, CreateTimeSlotRequest r) {
        courtAccess.requireCanManage(courtId);
        var court = requireCourt(courtId);
        validate(r.startTime(), r.endTime());
        if (repository.existsByCourtIdAndStartTimeAndEndTime(courtId, r.startTime(), r.endTime()))
            throw new ConflictException("Time slot already exists for this court");
        return map(repository.save(TimeSlot.builder().court(court).startTime(r.startTime()).endTime(r.endTime())
                .price(r.price()).build()));
    }

    public TimeSlotResponse update(Long courtId, Long id, UpdateTimeSlotRequest r) {
        courtAccess.requireCanManage(courtId);
        validate(r.startTime(), r.endTime());
        var s = get(courtId, id);
        if (repository.existsByCourtIdAndStartTimeAndEndTimeAndIdNot(courtId, r.startTime(), r.endTime(), id))
            throw new ConflictException("Time slot already exists for this court");
        s.setStartTime(r.startTime());
        s.setEndTime(r.endTime());
        s.setPrice(r.price());
        if (r.active() != null)
            s.setActive(r.active());
        return map(s);
    }

    public void delete(Long courtId, Long id) {
        courtAccess.requireCanManage(courtId);
        get(courtId, id).setActive(false);
    }

    private void validate(LocalTime start, LocalTime end) {
        if (!start.isBefore(end))
            throw new BadRequestException("Start time must be before end time");
    }

    private Court requireCourt(Long courtId) {
        return courts.findById(courtId).orElseThrow(() -> new NotFoundException("Court not found"));
    }

    private TimeSlot get(Long courtId, Long id) {
        return repository.findByIdAndCourtId(id, courtId)
                .orElseThrow(() -> new NotFoundException("Time slot not found"));
    }

    private TimeSlotResponse map(TimeSlot s) {
        return new TimeSlotResponse(s.getId(), s.getCourt().getId(), s.getStartTime(), s.getEndTime(), s.getPrice(),
                s.isActive());
    }
}
