package com.project.modules.timeslot.service;

import java.util.List;

import com.project.modules.timeslot.dto.request.*;
import com.project.modules.timeslot.dto.response.TimeSlotResponse;

public interface TimeSlotService {
    List<TimeSlotResponse> findByCourt(Long courtId);

    TimeSlotResponse create(Long courtId, CreateTimeSlotRequest r);

    List<TimeSlotResponse> createBulk(Long courtId, BulkCreateTimeSlotRequest r);

    TimeSlotResponse update(Long courtId, Long id, UpdateTimeSlotRequest r);

    void delete(Long courtId, Long id);
}
