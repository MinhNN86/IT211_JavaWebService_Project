package com.project.modules.court.service.impl;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.common.enums.CourtStatus;
import com.project.common.enums.RoleName;
import com.project.common.exception.BadRequestException;
import com.project.common.exception.ConflictException;
import com.project.common.exception.ForbiddenException;
import com.project.common.exception.NotFoundException;
import com.project.common.response.PageResponse;
import com.project.common.util.SecurityUtils;
import com.project.modules.court.dto.request.CreateCourtRequest;
import com.project.modules.court.dto.request.UpdateCourtRequest;
import com.project.modules.court.dto.response.CourtManagerResponse;
import com.project.modules.court.dto.response.CourtResponse;
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
    private final CourtRepository courtRepository;
    private final CourtMapper courtMapper;
    private final UserRepository userRepository;
    private final CourtAccessService courtAccessService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CourtResponse> findAll(String name, CourtStatus status, Pageable pageable) {
        Page<Court> courtPage = courtRepository.search(name, status, pageable);
        List<CourtResponse> courtResponses = courtPage.stream()
                .map(courtMapper::toResponse)
                .toList();

        return PageResponse.from(courtPage, courtResponses);
    }

    @Override
    @Transactional(readOnly = true)
    public CourtResponse findById(Long id) {
        Court court = findCourtById(id);
        return courtMapper.toResponse(court);
    }

    @Override
    public CourtResponse create(CreateCourtRequest request) {
        Set<User> managers = getManagersForNewCourt(request.managerIds());
        Court court = Court.builder()
                .name(request.name())
                .description(request.description())
                .address(request.address())
                .managers(managers)
                .build();

        Court savedCourt = courtRepository.save(court);
        return courtMapper.toResponse(savedCourt);
    }

    @Override
    public CourtResponse update(Long id, UpdateCourtRequest request) {
        courtAccessService.requireCanManage(id);
        Court court = findCourtById(id);

        court.setName(request.name());
        court.setDescription(request.description());
        court.setAddress(request.address());
        if (request.status() != null) {
            court.setStatus(request.status());
        }

        return courtMapper.toResponse(court);
    }

    @Override
    public void delete(Long id) {
        courtAccessService.requireCanManage(id);
        Court court = findCourtById(id);
        court.setStatus(CourtStatus.INACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourtManagerResponse> findManagers(Long courtId) {
        requireAdmin();
        Court court = findCourtById(courtId);

        return court.getManagers().stream()
                .map(this::toManagerResponse)
                .sorted(Comparator.comparing(CourtManagerResponse::username))
                .toList();
    }

    @Override
    public CourtManagerResponse addManager(Long courtId, UUID managerId) {
        requireAdmin();
        Court court = findCourtByIdForUpdate(courtId);
        User manager = findActiveManagerById(managerId);

        if (!court.getManagers().add(manager)) {
            throw new ConflictException("Manager is already assigned to this court");
        }

        return toManagerResponse(manager);
    }

    @Override
    public void removeManager(Long courtId, UUID managerId) {
        requireAdmin();
        Court court = findCourtByIdForUpdate(courtId);
        User manager = court.getManagers().stream()
                .filter(user -> user.getId().equals(managerId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Manager is not assigned to this court"));

        if (court.getManagers().size() == 1) {
            throw new BadRequestException("A court must have at least one manager");
        }

        court.getManagers().remove(manager);
    }

    private Set<User> getManagersForNewCourt(Set<UUID> requestedManagerIds) {
        User currentUser = userRepository.findByUsername(SecurityUtils.currentUsername())
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (currentUser.getRole() == RoleName.MANAGER) {
            boolean assignsAnotherManager = requestedManagerIds != null
                    && !requestedManagerIds.isEmpty()
                    && !(requestedManagerIds.size() == 1 && requestedManagerIds.contains(currentUser.getId()));
            if (assignsAnotherManager) {
                throw new ForbiddenException("Managers cannot assign other managers");
            }

            return new HashSet<>(Set.of(currentUser));
        }

        requireAdmin();
        if (requestedManagerIds == null || requestedManagerIds.isEmpty()) {
            throw new BadRequestException("At least one managerId is required when an admin creates a court");
        }

        List<User> managers = userRepository.findAllById(requestedManagerIds);
        if (managers.size() != requestedManagerIds.size()) {
            throw new NotFoundException("One or more managers not found");
        }

        boolean containsInvalidManager = managers.stream()
                .anyMatch(user -> user.getRole() != RoleName.MANAGER || !user.isActive());
        if (containsInvalidManager) {
            throw new BadRequestException("All assigned users must be active managers");
        }

        return new HashSet<>(managers);
    }

    private User findActiveManagerById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (user.getRole() != RoleName.MANAGER || !user.isActive()) {
            throw new BadRequestException("User must be an active manager");
        }

        return user;
    }

    private CourtManagerResponse toManagerResponse(User user) {
        return new CourtManagerResponse(user.getId(), user.getUsername(), user.getFullName());
    }

    private void requireAdmin() {
        if (!SecurityUtils.hasRole("ADMIN")) {
            throw new ForbiddenException("Admin access required");
        }
    }

    private Court findCourtById(Long id) {
        return courtRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Court not found"));
    }

    private Court findCourtByIdForUpdate(Long id) {
        return courtRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Court not found"));
    }
}
