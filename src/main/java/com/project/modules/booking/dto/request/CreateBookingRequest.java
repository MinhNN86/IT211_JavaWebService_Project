package com.project.modules.booking.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.*;

public record CreateBookingRequest(@NotNull Long courtId, @NotNull Long timeSlotId,
        @NotNull @FutureOrPresent LocalDate bookingDate, String note) {
}
