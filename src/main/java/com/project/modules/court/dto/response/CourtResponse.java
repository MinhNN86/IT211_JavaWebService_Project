package com.project.modules.court.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.project.common.enums.CourtStatus;

public record CourtResponse(Long id, String name, String description, String address, BigDecimal pricePerHour,
        CourtStatus status, List<String> images) {
}
