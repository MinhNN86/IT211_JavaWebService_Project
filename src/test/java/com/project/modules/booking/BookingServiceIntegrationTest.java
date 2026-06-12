package com.project.modules.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.*;
import java.util.List;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.project.common.exception.BadRequestException;
import com.project.common.exception.ConflictException;
import com.project.modules.booking.dto.request.CreateBookingRequest;
import com.project.modules.booking.repository.BookingRepository;
import com.project.modules.booking.service.BookingService;
import com.project.modules.court.entity.Court;
import com.project.modules.court.repository.CourtRepository;
import com.project.modules.timeslot.entity.TimeSlot;
import com.project.modules.timeslot.repository.TimeSlotRepository;

@SpringBootTest
@Transactional
class BookingServiceIntegrationTest {
    @Autowired
    private BookingService service;
    @Autowired
    private BookingRepository bookings;
    @Autowired
    private CourtRepository courts;
    @Autowired
    private TimeSlotRepository timeSlots;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("customer", null, List.of()));
        bookings.deleteAll();
        timeSlots.deleteAll();
        courts.deleteAll();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        bookings.deleteAll();
        timeSlots.deleteAll();
        courts.deleteAll();
    }

    @Test
    void createsOneBookingWithMultipleTimeSlots() {
        var court = createCourt();
        var first = createTimeSlot(court, LocalTime.of(18, 0), LocalTime.of(19, 0));
        var second = createTimeSlot(court, LocalTime.of(19, 0), LocalTime.of(20, 0));

        var response = service.create(request(court, List.of(first.getId(), second.getId())));

        assertThat(response.timeSlots()).extracting("id").containsExactly(first.getId(), second.getId());
        assertThat(bookings.findById(response.id()).orElseThrow().getTimeSlots()).hasSize(2);
    }

    @Test
    void rejectsBookingWhenAnySelectedTimeSlotIsAlreadyBooked() {
        var court = createCourt();
        var first = createTimeSlot(court, LocalTime.of(18, 0), LocalTime.of(19, 0));
        var second = createTimeSlot(court, LocalTime.of(19, 0), LocalTime.of(20, 0));
        service.create(request(court, List.of(first.getId())));

        assertThatThrownBy(() -> service.create(request(court, List.of(first.getId(), second.getId()))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void returnsBookedPricesAfterTimeSlotPriceChanges() {
        var court = createCourt();
        var first = createTimeSlot(court, LocalTime.of(18, 0), LocalTime.of(19, 0));
        var second = createTimeSlot(court, LocalTime.of(19, 0), LocalTime.of(20, 0));

        var booking = service.create(request(court, List.of(first.getId(), second.getId())));
        first.setPrice(75_000);
        timeSlots.save(first);

        var response = service.myBookings(Pageable.unpaged()).content().stream()
                .filter(item -> item.id().equals(booking.id()))
                .findFirst()
                .orElseThrow();
        var snapshot = response.priceSnapshot();

        assertThat(snapshot.get("timeSlots")).hasSize(2);
        assertThat(snapshot.at("/timeSlots/0/price").asInt()).isEqualTo(50_000);
        assertThat(snapshot.get("totalPrice").asLong()).isEqualTo(100_000);
        assertThat(response.timeSlots()).extracting("price").containsExactly(50_000, 50_000);
    }

    @Test
    void rejectsDuplicateTimeSlotIds() {
        var court = createCourt();
        var slot = createTimeSlot(court, LocalTime.of(18, 0), LocalTime.of(19, 0));

        assertThatThrownBy(() -> service.create(request(court, List.of(slot.getId(), slot.getId()))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsTimeSlotFromAnotherCourt() {
        var selectedCourt = createCourt();
        var otherCourt = createCourt();
        var otherCourtSlot = createTimeSlot(otherCourt, LocalTime.of(18, 0), LocalTime.of(19, 0));

        assertThatThrownBy(() -> service.create(request(selectedCourt, List.of(otherCourtSlot.getId()))))
                .isInstanceOf(BadRequestException.class);
    }

    private CreateBookingRequest request(Court court, List<Long> timeSlotIds) {
        return new CreateBookingRequest(court.getId(), timeSlotIds, LocalDate.now().plusDays(1), null);
    }

    private Court createCourt() {
        return courts.save(Court.builder().name("Court 1").address("Address").build());
    }

    private TimeSlot createTimeSlot(Court court, LocalTime start, LocalTime end) {
        return timeSlots.save(TimeSlot.builder().court(court).startTime(start).endTime(end)
                .price(50_000).build());
    }
}
