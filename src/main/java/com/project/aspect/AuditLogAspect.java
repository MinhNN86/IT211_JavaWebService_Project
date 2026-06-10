package com.project.aspect;

import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import com.project.common.util.SecurityUtils;
import com.project.modules.audit.service.AuditLogService;
import com.project.modules.booking.dto.request.CreateBookingRequest;
import com.project.modules.booking.dto.response.BookingResponse;

import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {
    private final AuditLogService audit;

    @AfterReturning(pointcut = "execution(* com.project.modules.booking.service.impl.BookingServiceImpl.create(..))", returning = "result")
    public void success(BookingResponse result) {
        audit.log(result.customerUsername(), "CREATE_BOOKING", "Booked court " + result.courtId() + " on "
                + result.bookingDate() + " for time slots "
                + result.timeSlots().stream().map(slot -> slot.startTime() + "-" + slot.endTime()).toList(), "SUCCESS");
    }

    @AfterThrowing(pointcut = "execution(* com.project.modules.booking.service.impl.BookingServiceImpl.create(..)) && args(request)", throwing = "error")
    public void failed(CreateBookingRequest request, Throwable error) {
        String username;
        try {
            username = SecurityUtils.currentUsername();
        } catch (Exception ex) {
            username = "anonymous";
        }
        audit.log(username, "CREATE_BOOKING", "Failed to book court " + request.courtId() + ": " + error.getMessage(),
                "FAILED");
    }
}
