package com.project.modules.court.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.project.common.response.ApiResponse;
import com.project.modules.court.dto.response.CourtManagerResponse;
import com.project.modules.court.service.CourtService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/courts/{courtId}/managers")
@RequiredArgsConstructor
public class AdminCourtManagerController {
    private final CourtService service;

    @GetMapping
    ApiResponse<List<CourtManagerResponse>> all(@PathVariable Long courtId) {
        return ApiResponse.success("Court managers retrieved", service.findManagers(courtId));
    }

    @PostMapping("/{managerId}")
    ApiResponse<CourtManagerResponse> add(@PathVariable Long courtId, @PathVariable UUID managerId) {
        return ApiResponse.success("Court manager added", service.addManager(courtId, managerId));
    }

    @DeleteMapping("/{managerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@PathVariable Long courtId, @PathVariable UUID managerId) {
        service.removeManager(courtId, managerId);
    }
}
