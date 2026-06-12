package com.project.modules.timeslot.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import com.project.common.response.ApiResponse;
import com.project.modules.timeslot.dto.request.*;
import com.project.modules.timeslot.dto.response.TimeSlotResponse;
import com.project.modules.timeslot.service.TimeSlotService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TimeSlotController {
    private final TimeSlotService service;

    @GetMapping("/api/v1/courts/{courtId}/time-slots")
    ApiResponse<List<TimeSlotResponse>> all(@PathVariable Long courtId) {
        return ApiResponse.success("Time slots retrieved", service.findByCourt(courtId));
    }

    @PostMapping("/api/v1/manager/courts/{courtId}/time-slots")
    ResponseEntity<ApiResponse<TimeSlotResponse>> create(@PathVariable Long courtId,
            @Valid @RequestBody CreateTimeSlotRequest r) {
        return ResponseEntity.status(201).body(ApiResponse.success("Time slot created", service.create(courtId, r)));
    }

    @PostMapping("/api/v1/manager/courts/{courtId}/time-slots/bulk")
    ResponseEntity<ApiResponse<List<TimeSlotResponse>>> createBulk(@PathVariable Long courtId,
            @Valid @RequestBody BulkCreateTimeSlotRequest r) {
        return ResponseEntity.status(201)
                .body(ApiResponse.success("Time slots created", service.createBulk(courtId, r)));
    }

    @PatchMapping("/api/v1/manager/time-slots/prices")
    ApiResponse<List<TimeSlotResponse>> updatePrices(@Valid @RequestBody BulkUpdatePriceRequest r) {
        return ApiResponse.success("Prices updated", service.updatePrices(r));
    }

    @PutMapping("/api/v1/manager/courts/{courtId}/time-slots/{id}")
    ApiResponse<TimeSlotResponse> update(@PathVariable Long courtId, @PathVariable Long id,
            @Valid @RequestBody UpdateTimeSlotRequest r) {
        return ApiResponse.success("Time slot updated", service.update(courtId, id, r));
    }

    @DeleteMapping("/api/v1/manager/courts/{courtId}/time-slots/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long courtId, @PathVariable Long id) {
        service.delete(courtId, id);
    }
}
