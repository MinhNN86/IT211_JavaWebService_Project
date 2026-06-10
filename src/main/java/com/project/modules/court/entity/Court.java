package com.project.modules.court.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import jakarta.persistence.*;

import com.project.common.enums.CourtStatus;
import com.project.modules.user.entity.User;

import lombok.*;

@Entity
@Table(name = "courts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Court {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(length = 2000)
    private String description;
    @Column(nullable = false)
    private String address;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerHour;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CourtStatus status = CourtStatus.ACTIVE;
    @OneToMany(mappedBy = "court", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<CourtImage> images = new ArrayList<>();
    @ManyToMany
    @JoinTable(name = "court_managers", joinColumns = @JoinColumn(name = "court_id"), inverseJoinColumns = @JoinColumn(name = "manager_id"))
    @Builder.Default
    private Set<User> managers = new HashSet<>();
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
