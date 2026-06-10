package com.project.modules.booking.repository;

import java.time.LocalDate;
import java.util.Collection;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.common.enums.BookingStatus;
import com.project.modules.booking.entity.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("""
            select case when count(b) > 0 then true else false end
            from Booking b join b.timeSlots ts
            where b.court.id = :courtId
              and b.bookingDate = :date
              and ts.id in :slotIds
              and b.status in :statuses
            """)
    boolean existsBlockingBooking(@Param("courtId") Long courtId, @Param("date") LocalDate date,
            @Param("slotIds") Collection<Long> slotIds, @Param("statuses") Collection<BookingStatus> statuses);

    Page<Booking> findByCustomerUsername(String username, Pageable pageable);

    @Query("select b from Booking b join b.court.managers manager where manager.username = :username")
    Page<Booking> findManagedBookings(@Param("username") String username, Pageable pageable);
}
