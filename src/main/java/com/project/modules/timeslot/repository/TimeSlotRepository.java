package com.project.modules.timeslot.repository;

import java.time.LocalTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.modules.timeslot.entity.TimeSlot;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    boolean existsByStartTimeAndEndTime(LocalTime startTime, LocalTime endTime);
}
