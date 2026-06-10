package com.project.modules.court.service.impl;

import java.math.BigDecimal;
import java.util.*;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.common.enums.*;
import com.project.common.exception.*;
import com.project.common.response.PageResponse;
import com.project.common.util.SecurityUtils;
import com.project.modules.court.dto.request.*;
import com.project.modules.court.dto.response.*;
import com.project.modules.court.entity.Court;
import com.project.modules.court.mapper.CourtMapper;
import com.project.modules.court.repository.CourtRepository;
import com.project.modules.court.service.CourtAccessService;
import com.project.modules.court.service.CourtService;
import com.project.modules.user.entity.User;
import com.project.modules.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CourtServiceImpl implements CourtService {
    private final CourtRepository repository;
    private final CourtMapper mapper;
    private final UserRepository users;
    private final CourtAccessService access;
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
        var managers = managersForNewCourt(r.managerIds());
        return mapper.toResponse(repository.save(Court.builder().name(r.name()).description(r.description())
                .address(r.address()).pricePerHour(r.pricePerHour()).managers(managers).build()));
    }

    public CourtResponse update(Long id, UpdateCourtRequest r) {
        access.requireCanManage(id);
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
        access.requireCanManage(id);
        get(id).setStatus(CourtStatus.INACTIVE);
    }

    @Transactional(readOnly = true)
    public List<CourtManagerResponse> findManagers(Long courtId) {
        requireAdmin();
        return get(courtId).getManagers().stream().map(this::managerResponse)
                .sorted(Comparator.comparing(CourtManagerResponse::username)).toList();
    }

    public CourtManagerResponse addManager(Long courtId, UUID managerId) {
        requireAdmin();
        var court = getForUpdate(courtId);
        var manager = getManager(managerId);
        if (!court.getManagers().add(manager))
            throw new ConflictException("Manager is already assigned to this court");
        return managerResponse(manager);
    }

    public void removeManager(Long courtId, UUID managerId) {
        requireAdmin();
        var court = getForUpdate(courtId);
        var manager = court.getManagers().stream().filter(user -> user.getId().equals(managerId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Manager is not assigned to this court"));
        if (court.getManagers().size() == 1)
            throw new BadRequestException("A court must have at least one manager");
        court.getManagers().remove(manager);
    }

    private Set<User> managersForNewCourt(Set<UUID> requestedManagerIds) {
        var current = users.findByUsername(SecurityUtils.currentUsername())
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (current.getRole() == RoleName.MANAGER) {
            if (requestedManagerIds != null && !requestedManagerIds.isEmpty()
                    && !(requestedManagerIds.size() == 1 && requestedManagerIds.contains(current.getId())))
                throw new ForbiddenException("Managers cannot assign other managers");
            return new HashSet<>(Set.of(current));
        }
        requireAdmin();
        if (requestedManagerIds == null || requestedManagerIds.isEmpty())
            throw new BadRequestException("At least one managerId is required when an admin creates a court");
        var managers = users.findAllById(requestedManagerIds);
        if (managers.size() != requestedManagerIds.size())
            throw new NotFoundException("One or more managers not found");
        if (managers.stream()
                .anyMatch(user -> user.getRole() != RoleName.MANAGER || user.getStatus() != UserStatus.ACTIVE))
            throw new BadRequestException("All assigned users must be active managers");
        return new HashSet<>(managers);
    }

    private User getManager(UUID id) {
        var user = users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() != RoleName.MANAGER || user.getStatus() != UserStatus.ACTIVE)
            throw new BadRequestException("User must be an active manager");
        return user;
    }

    private CourtManagerResponse managerResponse(User user) {
        return new CourtManagerResponse(user.getId(), user.getUsername(), user.getFullName());
    }

    private void requireAdmin() {
        if (!SecurityUtils.hasRole("ADMIN"))
            throw new ForbiddenException("Admin access required");
    }

    private Court get(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Court not found"));
    }

    private Court getForUpdate(Long id) {
        return repository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Court not found"));
    }
}
