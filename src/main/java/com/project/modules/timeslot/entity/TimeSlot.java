package com.project.modules.timeslot.entity;

import java.math.BigDecimal;
import java.time.LocalTime;

import jakarta.persistence.*;

import lombok.*;

@Entity
@Table(name = "time_slots", uniqueConstraints = @UniqueConstraint(columnNames = {"startTime", "endTime"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private LocalTime startTime;
    @Column(nullable = false)
    private LocalTime endTime;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
