package com.project.modules.booking.dto.request;

import jakarta.validation.constraints.NotNull;

import com.project.common.enums.BookingStatus;

public record UpdateBookingStatusRequest(@NotNull BookingStatus status) {
}
