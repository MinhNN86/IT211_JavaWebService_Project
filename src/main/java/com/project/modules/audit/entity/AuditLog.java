package com.project.modules.audit.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.*;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    @Column(nullable = false)
    private String action;
    @Column(length = 2000)
    private String message;
    @Column(nullable = false)
    private String status;
    private LocalDateTime createdAt;
    @PrePersist
    void create() {
        createdAt = LocalDateTime.now();
    }
}
