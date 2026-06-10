package com.project.modules.court.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.*;

@Entity
@Table(name = "court_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourtImage {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;
    @Column(nullable = false)
    private String fileName;
    @Column(nullable = false)
    private String url;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void create() {
        if (createdAt == null)
            createdAt = LocalDateTime.now();
    }
}
