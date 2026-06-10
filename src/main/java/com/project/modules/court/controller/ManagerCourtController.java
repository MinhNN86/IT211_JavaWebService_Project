package com.project.modules.court.controller;

import jakarta.validation.Valid;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import com.project.common.response.ApiResponse;
import com.project.modules.court.dto.request.*;
import com.project.modules.court.dto.response.CourtResponse;
import com.project.modules.court.service.CourtService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/manager/courts")
@RequiredArgsConstructor
public class ManagerCourtController {
    private final CourtService service;
    @PostMapping
    ResponseEntity<ApiResponse<CourtResponse>> create(@Valid @RequestBody CreateCourtRequest r) {
        return ResponseEntity.status(201).body(ApiResponse.success("Court created", service.create(r)));
    }

    @PutMapping("/{id}")
    ApiResponse<CourtResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateCourtRequest r) {
        return ApiResponse.success("Court updated", service.update(id, r));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
