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
    @GetMapping("/api/v1/time-slots")
    ApiResponse<List<TimeSlotResponse>> all() {
        return ApiResponse.success("Time slots retrieved", service.findAll());
    }

    @PostMapping("/api/v1/admin/time-slots")
    ResponseEntity<ApiResponse<TimeSlotResponse>> create(@Valid @RequestBody CreateTimeSlotRequest r) {
        return ResponseEntity.status(201).body(ApiResponse.success("Time slot created", service.create(r)));
    }

    @PutMapping("/api/v1/admin/time-slots/{id}")
    ApiResponse<TimeSlotResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateTimeSlotRequest r) {
        return ApiResponse.success("Time slot updated", service.update(id, r));
    }

    @DeleteMapping("/api/v1/admin/time-slots/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
