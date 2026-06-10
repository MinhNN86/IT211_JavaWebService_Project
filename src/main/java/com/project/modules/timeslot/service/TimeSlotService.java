package com.project.modules.timeslot.service;

import java.util.List;

import com.project.modules.timeslot.dto.request.*;
import com.project.modules.timeslot.dto.response.TimeSlotResponse;

public interface TimeSlotService {
    List<TimeSlotResponse> findAll();

    TimeSlotResponse create(CreateTimeSlotRequest r);

    TimeSlotResponse update(Long id, UpdateTimeSlotRequest r);

    void delete(Long id);
}
