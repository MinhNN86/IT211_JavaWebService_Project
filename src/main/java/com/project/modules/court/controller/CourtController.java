package com.project.modules.court.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import com.project.common.enums.CourtStatus;
import com.project.common.response.*;
import com.project.modules.court.dto.response.CourtDetailResponse;
import com.project.modules.court.dto.response.CourtResponse;
import com.project.modules.court.service.CourtService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/courts")
@RequiredArgsConstructor
public class CourtController {
    private final CourtService service;
    @GetMapping
    ApiResponse<PageResponse<CourtResponse>> all(@RequestParam(required = false) String name,
            @RequestParam(required = false) CourtStatus status, Pageable p) {
        return ApiResponse.success("Courts retrieved", service.findAll(name, status, p));
    }

    @GetMapping("/{id}")
    ApiResponse<CourtDetailResponse> one(@PathVariable Long id) {
        return ApiResponse.success("Court retrieved", service.findById(id));
    }
}
