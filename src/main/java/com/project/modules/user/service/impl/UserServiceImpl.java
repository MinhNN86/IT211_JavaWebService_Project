package com.project.modules.user.service.impl;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.common.enums.RoleName;
import com.project.common.exception.ConflictException;
import com.project.common.exception.NotFoundException;
import com.project.common.response.PageResponse;
import com.project.common.util.SecurityUtils;
import com.project.modules.audit.repository.AuditLogRepository;
import com.project.modules.auth.repository.RefreshTokenRepository;
import com.project.modules.booking.repository.BookingRepository;
import com.project.modules.court.repository.CourtRepository;
import com.project.modules.user.dto.request.CreateUserRequest;
import com.project.modules.user.dto.request.UpdateProfileRequest;
import com.project.modules.user.dto.request.UpdateUserRequest;
import com.project.modules.user.dto.response.UserResponse;
import com.project.modules.user.entity.User;
import com.project.modules.user.mapper.UserMapper;
import com.project.modules.user.repository.UserRepository;
import com.project.modules.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CourtRepository courtRepository;
    private final BookingRepository bookingRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> findAll(String keyword, Pageable pageable) {
        Page<User> userPage = userRepository.search(keyword, pageable);
        return PageResponse.from(userPage, userPage.stream().map(userMapper::toResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        User user = getUserById(id);
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse create(CreateUserRequest request) {
        boolean usernameExists = userRepository.existsByUsername(request.username());
        boolean emailExists = userRepository.existsByEmail(request.email());

        if (usernameExists || emailExists) {
            throw new ConflictException("Username or email already exists");
        }

        RoleName role = request.role() != null ? request.role() : RoleName.CUSTOMER;
        User user = User.builder()
                .fullName(request.fullName())
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .role(role)
                .build();

        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = getUserById(id);
        requireNotAssignedToCourt(user, request.role());

        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());

        if (request.isActive() != null) {
            user.setActive(request.isActive());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }

        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse disable(UUID id) {
        User user = getUserById(id);
        user.setActive(false);
        refreshTokenRepository.revokeByUserId(id);
        return userMapper.toResponse(user);
    }

    @Override
    public void delete(UUID id) {
        User user = getUserById(id);

        bookingRepository.deleteAll(bookingRepository.findAllByCustomerId(id));
        refreshTokenRepository.deleteByUserId(id);
        courtRepository.findAllByManagersId(id).forEach(court -> court.getManagers().remove(user));
        auditLogRepository.deleteByUsername(user.getUsername());
        userRepository.delete(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse profile() {
        User currentUser = getCurrentUser();
        return userMapper.toResponse(currentUser);
    }

    @Override
    public UserResponse updateProfile(UpdateProfileRequest request) {
        User currentUser = getCurrentUser();

        currentUser.setFullName(request.fullName());
        currentUser.setEmail(request.email());
        currentUser.setPhone(request.phone());

        return userMapper.toResponse(currentUser);
    }

    private User getUserById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }

    private User getCurrentUser() {
        String currentUsername = SecurityUtils.currentUsername();
        return userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private void requireNotAssignedToCourt(User user, RoleName newRole) {
        boolean losesManagerAccess = newRole != null && newRole != RoleName.MANAGER;
        if (losesManagerAccess && courtRepository.existsByManagersId(user.getId())) {
            throw new ConflictException("Remove this manager from all courts before changing role");
        }
    }
}
