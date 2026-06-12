package com.project.modules.booking.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.*;

public record CreateBookingRequest(
        @NotNull Long courtId,
        @NotEmpty List<@NotNull Long> timeSlotIds,
        @NotNull @FutureOrPresent LocalDate bookingDate,
        String note) {
}
