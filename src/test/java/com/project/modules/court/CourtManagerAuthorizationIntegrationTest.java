package com.project.modules.court;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.project.common.enums.*;
import com.project.common.exception.*;
import com.project.modules.booking.dto.request.UpdateBookingStatusRequest;
import com.project.modules.booking.entity.Booking;
import com.project.modules.booking.repository.BookingRepository;
import com.project.modules.booking.service.BookingService;
import com.project.modules.court.dto.request.*;
import com.project.modules.court.repository.CourtRepository;
import com.project.modules.court.service.CourtService;
import com.project.modules.user.dto.request.UpdateUserRequest;
import com.project.modules.user.entity.User;
import com.project.modules.user.repository.UserRepository;
import com.project.modules.user.service.UserService;

@SpringBootTest
@Transactional
class CourtManagerAuthorizationIntegrationTest {
    @Autowired
    private CourtService courtService;
    @Autowired
    private BookingService bookingService;
    @Autowired
    private CourtRepository courts;
    @Autowired
    private BookingRepository bookings;
    @Autowired
    private UserRepository users;
    @Autowired
    private UserService userService;

    private User firstManager;
    private User secondManager;
    private User admin;
    private User customer;

    @BeforeEach
    void setUp() {
        firstManager = createUser("manager-one", RoleName.MANAGER);
        secondManager = createUser("manager-two", RoleName.MANAGER);
        admin = createUser("court-admin", RoleName.ADMIN);
        customer = createUser("booking-customer", RoleName.CUSTOMER);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void managerCanOnlyUpdateAssignedCourt() {
        authenticate(firstManager);
        var court = courtService.create(createRequest(null));

        authenticate(secondManager);
        assertThatThrownBy(() -> courtService.update(court.id(), updateRequest("Unauthorized update")))
                .isInstanceOf(ForbiddenException.class);

        authenticate(firstManager);
        assertThat(courtService.update(court.id(), updateRequest("Authorized update")).name())
                .isEqualTo("Authorized update");
    }

    @Test
    void adminCanAssignMultipleManagersButCannotRemoveLastManager() {
        authenticate(firstManager);
        var court = courtService.create(createRequest(null));

        authenticate(admin);
        courtService.addManager(court.id(), secondManager.getId());
        assertThat(courtService.findManagers(court.id())).extracting("username")
                .containsExactlyInAnyOrder(firstManager.getUsername(), secondManager.getUsername());
        courtService.removeManager(court.id(), firstManager.getId());

        authenticate(secondManager);
        assertThat(courtService.update(court.id(), updateRequest("Managed by second")).name())
                .isEqualTo("Managed by second");

        authenticate(admin);
        assertThatThrownBy(() -> courtService.removeManager(court.id(), secondManager.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void managerOnlySeesAndUpdatesBookingsForAssignedCourts() {
        var firstCourt = createCourtFor(firstManager, "First court");
        var secondCourt = createCourtFor(secondManager, "Second court");
        var firstBooking = bookings.save(booking(firstCourt.id()));
        var secondBooking = bookings.save(booking(secondCourt.id()));

        authenticate(firstManager);
        assertThat(bookingService.all(Pageable.unpaged()).content()).extracting("id")
                .containsExactly(firstBooking.getId());
        assertThatThrownBy(() -> bookingService.updateStatus(secondBooking.getId(),
                new UpdateBookingStatusRequest(BookingStatus.CONFIRMED))).isInstanceOf(ForbiddenException.class);
        assertThat(bookingService.updateStatus(firstBooking.getId(),
                new UpdateBookingStatusRequest(BookingStatus.CONFIRMED)).status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void adminMustAssignAtLeastOneManagerWhenCreatingCourt() {
        authenticate(admin);

        assertThatThrownBy(() -> courtService.create(createRequest(null))).isInstanceOf(BadRequestException.class);
        assertThat(courtService.create(createRequest(java.util.Set.of(firstManager.getId()))).id()).isNotNull();
    }

    @Test
    void assignedManagerCannotBeDemotedButCanBeDisabledAndDeleted() {
        createCourtFor(firstManager, "Managed court");
        authenticate(admin);

        assertThatThrownBy(() -> userService.update(firstManager.getId(),
                new UpdateUserRequest(firstManager.getFullName(), firstManager.getEmail(), null, false,
                        RoleName.CUSTOMER)))
                .isInstanceOf(ConflictException.class);
        assertThat(userService.disable(firstManager.getId()).isActive()).isFalse();

        userService.delete(firstManager.getId());

        assertThat(users.existsById(firstManager.getId())).isFalse();
        assertThat(courts.existsByManagersId(firstManager.getId())).isFalse();
    }

    private com.project.modules.court.dto.response.CourtResponse createCourtFor(User manager, String name) {
        authenticate(manager);
        return courtService.create(new CreateCourtRequest(name, null, "Address", null));
    }

    private Booking booking(Long courtId) {
        return Booking.builder().customer(customer).court(courts.findById(courtId).orElseThrow())
                .bookingDate(LocalDate.now().plusDays(1)).build();
    }

    private CreateCourtRequest createRequest(java.util.Set<UUID> managerIds) {
        return new CreateCourtRequest("Court", null, "Address", managerIds);
    }

    private UpdateCourtRequest updateRequest(String name) {
        return new UpdateCourtRequest(name, null, "Address", CourtStatus.ACTIVE);
    }

    private User createUser(String prefix, RoleName role) {
        var suffix = UUID.randomUUID().toString();
        return users.save(User.builder().fullName(prefix).username(prefix + "-" + suffix)
                .email(prefix + "-" + suffix + "@test.local").password("password").role(role).build());
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user.getUsername(),
                null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))));
    }
}
