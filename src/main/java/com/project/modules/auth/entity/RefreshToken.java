package com.project.modules.auth.entity;

import java.time.Instant;
import java.time.LocalDateTime;

import jakarta.persistence.*;

import com.project.modules.user.entity.User;

import lombok.*;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 1000)
    private String token;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;
    @Column(nullable = false)
    private Instant expiryDate;
    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;
    private LocalDateTime createdAt;
    @PrePersist
    void create() {
        createdAt = LocalDateTime.now();
    }
}
