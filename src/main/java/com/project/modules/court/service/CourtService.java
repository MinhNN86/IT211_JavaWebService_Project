package com.project.modules.court.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Pageable;

import com.project.common.enums.CourtStatus;
import com.project.common.response.PageResponse;
import com.project.modules.court.dto.request.*;
import com.project.modules.court.dto.response.CourtResponse;

public interface CourtService {
    PageResponse<CourtResponse> findAll(String name, CourtStatus status, BigDecimal minPrice, BigDecimal maxPrice,
            Pageable pageable);

    CourtResponse findById(Long id);

    CourtResponse create(CreateCourtRequest request);

    CourtResponse update(Long id, UpdateCourtRequest request);

    void delete(Long id);
}
