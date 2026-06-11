package com.project.modules.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.project.common.enums.RoleName;
import com.project.modules.audit.entity.AuditLog;
import com.project.modules.audit.repository.AuditLogRepository;
import com.project.modules.auth.entity.RefreshToken;
import com.project.modules.auth.repository.RefreshTokenRepository;
import com.project.modules.booking.entity.Booking;
import com.project.modules.booking.repository.BookingRepository;
import com.project.modules.court.entity.Court;
import com.project.modules.court.repository.CourtRepository;
import com.project.modules.timeslot.entity.TimeSlot;
import com.project.modules.timeslot.repository.TimeSlotRepository;
import com.project.modules.user.entity.User;
import com.project.modules.user.repository.UserRepository;
import com.project.modules.user.service.UserService;

@SpringBootTest
@Transactional
class UserServiceIntegrationTest {
    @Autowired
    private UserService service;
    @Autowired
    private UserRepository users;
    @Autowired
    private RefreshTokenRepository refreshTokens;
    @Autowired
    private BookingRepository bookings;
    @Autowired
    private CourtRepository courts;
    @Autowired
    private TimeSlotRepository timeSlots;
    @Autowired
    private AuditLogRepository auditLogs;
    @Autowired
    private EntityManager entityManager;

    @Test
    void disableDeactivatesUserAndRevokesRefreshTokens() {
        var user = createUser(RoleName.CUSTOMER);
        createRefreshToken(user);
        createRefreshToken(user);

        var response = service.disable(user.getId());
        flushAndClear();

        assertThat(response.isActive()).isFalse();
        assertThat(users.findById(user.getId()).orElseThrow().isActive()).isFalse();
        assertThat(refreshTokens.existsByUserId(user.getId())).isTrue();
        assertThat(refreshTokens.existsByUserIdAndRevokedFalse(user.getId())).isFalse();
    }

    @Test
    void deleteRemovesUserAndRelatedData() {
        var user = createUser(RoleName.MANAGER);
        var court = courts.save(Court.builder().name("Managed court").address("Address")
                .managers(new HashSet<>(java.util.Set.of(user))).build());
        var timeSlot = timeSlots.save(TimeSlot.builder().court(court).startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0)).price(100_000).build());
        bookings.save(Booking.builder().customer(user).court(court).bookingDate(LocalDate.now().plusDays(1))
                .timeSlots(new LinkedHashSet<>(java.util.Set.of(timeSlot))).build());
        createRefreshToken(user);
        auditLogs.save(AuditLog.builder().username(user.getUsername()).action("TEST").status("SUCCESS").build());

        service.delete(user.getId());
        flushAndClear();

        assertThat(users.existsById(user.getId())).isFalse();
        assertThat(refreshTokens.existsByUserId(user.getId())).isFalse();
        assertThat(bookings.existsByCustomerId(user.getId())).isFalse();
        assertThat(courts.existsByManagersId(user.getId())).isFalse();
        assertThat(auditLogs.existsByUsername(user.getUsername())).isFalse();
    }

    private User createUser(RoleName role) {
        var suffix = UUID.randomUUID().toString();
        return users.save(User.builder().fullName("User").username("user-" + suffix)
                .email("user-" + suffix + "@test.local").password("password").role(role).build());
    }

    private void createRefreshToken(User user) {
        refreshTokens.save(RefreshToken.builder().token(UUID.randomUUID().toString()).user(user)
                .expiryDate(Instant.now().plusSeconds(3600)).build());
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
