package com.project.modules.booking.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.*;

import com.project.common.enums.BookingStatus;
import com.project.modules.court.entity.Court;
import com.project.modules.timeslot.entity.TimeSlot;
import com.project.modules.user.entity.User;

import lombok.*;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User customer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Court court;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "booking_time_slots", joinColumns = @JoinColumn(name = "booking_id"), inverseJoinColumns = @JoinColumn(name = "time_slot_id"))
    @Builder.Default
    private Set<TimeSlot> timeSlots = new LinkedHashSet<>();
    @Column(nullable = false)
    private LocalDate bookingDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @PrePersist
    void create() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void update() {
        updatedAt = LocalDateTime.now();
    }
}
