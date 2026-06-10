package com.project.modules.booking.repository;

import java.time.LocalDate;
import java.util.Collection;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import com.project.common.enums.BookingStatus;
import com.project.modules.booking.entity.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    boolean existsByCourtIdAndBookingDateAndTimeSlotIdAndStatusIn(Long courtId, LocalDate date, Long slotId,
            Collection<BookingStatus> statuses);

    Page<Booking> findByCustomerUsername(String username, Pageable pageable);
}
