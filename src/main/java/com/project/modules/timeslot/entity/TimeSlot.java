package com.project.modules.timeslot.entity;

import java.math.BigDecimal;
import java.time.LocalTime;

import jakarta.persistence.*;

import com.project.modules.court.entity.Court;

import lombok.*;

@Entity
@Table(name = "time_slots", uniqueConstraints = @UniqueConstraint(name = "uk_time_slots_court_start_end", columnNames = {
        "court_id", "start_time", "end_time"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
