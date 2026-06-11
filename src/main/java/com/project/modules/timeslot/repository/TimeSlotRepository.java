package com.project.modules.timeslot.repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.modules.timeslot.entity.TimeSlot;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findByCourtIdOrderByStartTime(Long courtId);

    Optional<TimeSlot> findByIdAndCourtId(Long id, Long courtId);

    Optional<TimeSlot> findByCourtIdAndStartTimeAndEndTime(Long courtId, LocalTime startTime, LocalTime endTime);

    boolean existsByCourtIdAndActiveTrueAndStartTimeLessThanAndEndTimeGreaterThan(
            Long courtId, LocalTime endTime, LocalTime startTime);

    boolean existsByCourtIdAndActiveTrueAndIdNotAndStartTimeLessThanAndEndTimeGreaterThan(
            Long courtId, Long id, LocalTime endTime, LocalTime startTime);

    boolean existsByCourtIdAndIdNotAndStartTimeAndEndTime(
            Long courtId, Long id, LocalTime startTime, LocalTime endTime);
}
