package com.project.modules.court.dto.response;

import java.util.List;

import com.project.common.enums.CourtStatus;

public record CourtResponse(
        Long id,
        String name,
        String description,
        String address,
        CourtStatus status,
        List<CourtImageResponse> images) {
}
