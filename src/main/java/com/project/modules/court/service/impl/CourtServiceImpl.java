package com.project.modules.court.service.impl;

import java.math.BigDecimal;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.common.enums.CourtStatus;
import com.project.common.exception.NotFoundException;
import com.project.common.response.PageResponse;
import com.project.modules.court.dto.request.*;
import com.project.modules.court.dto.response.CourtResponse;
import com.project.modules.court.entity.Court;
import com.project.modules.court.mapper.CourtMapper;
import com.project.modules.court.repository.CourtRepository;
import com.project.modules.court.service.CourtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CourtServiceImpl implements CourtService {
    private final CourtRepository repository;
    private final CourtMapper mapper;
    @Transactional(readOnly = true)
    public PageResponse<CourtResponse> findAll(String name, CourtStatus status, BigDecimal minPrice,
            BigDecimal maxPrice, Pageable p) {
        Page<Court> page = repository.search(name, status, minPrice, maxPrice, p);
        return PageResponse.from(page, page.stream().map(mapper::toResponse).toList());
    }

    @Transactional(readOnly = true)
    public CourtResponse findById(Long id) {
        return mapper.toResponse(get(id));
    }

    public CourtResponse create(CreateCourtRequest r) {
        return mapper.toResponse(repository.save(Court.builder().name(r.name()).description(r.description())
                .address(r.address()).pricePerHour(r.pricePerHour()).build()));
    }

    public CourtResponse update(Long id, UpdateCourtRequest r) {
        var c = get(id);
        c.setName(r.name());
        c.setDescription(r.description());
        c.setAddress(r.address());
        c.setPricePerHour(r.pricePerHour());
        if (r.status() != null)
            c.setStatus(r.status());
        return mapper.toResponse(c);
    }

    public void delete(Long id) {
        get(id).setStatus(CourtStatus.INACTIVE);
    }

    private Court get(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Court not found"));
    }
}
