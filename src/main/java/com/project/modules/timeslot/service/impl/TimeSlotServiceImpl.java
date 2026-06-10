package com.project.modules.timeslot.service.impl;

import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.common.exception.*;
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
    @Transactional(readOnly = true)
    public List<TimeSlotResponse> findAll() {
        return repository.findAll().stream().map(this::map).toList();
    }

    public TimeSlotResponse create(CreateTimeSlotRequest r) {
        validate(r.startTime(), r.endTime());
        if (repository.existsByStartTimeAndEndTime(r.startTime(), r.endTime()))
            throw new ConflictException("Time slot already exists");
        return map(repository
                .save(TimeSlot.builder().startTime(r.startTime()).endTime(r.endTime()).price(r.price()).build()));
    }

    public TimeSlotResponse update(Long id, UpdateTimeSlotRequest r) {
        validate(r.startTime(), r.endTime());
        var s = get(id);
        s.setStartTime(r.startTime());
        s.setEndTime(r.endTime());
        s.setPrice(r.price());
        if (r.active() != null)
            s.setActive(r.active());
        return map(s);
    }

    public void delete(Long id) {
        get(id).setActive(false);
    }

    private void validate(LocalTime start, LocalTime end) {
        if (!start.isBefore(end))
            throw new BadRequestException("Start time must be before end time");
    }

    private TimeSlot get(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Time slot not found"));
    }

    private TimeSlotResponse map(TimeSlot s) {
        return new TimeSlotResponse(s.getId(), s.getStartTime(), s.getEndTime(), s.getPrice(), s.isActive());
    }
}
