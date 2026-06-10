package com.project.modules.user.service.impl;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.common.enums.*;
import com.project.common.exception.*;
import com.project.common.response.PageResponse;
import com.project.common.util.SecurityUtils;
import com.project.modules.user.dto.request.*;
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
    private final UserRepository users;
    private final UserMapper mapper;
    private final PasswordEncoder encoder;
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> findAll(String keyword, Pageable pageable) {
        var page = users.search(keyword, pageable);
        return PageResponse.from(page, page.stream().map(mapper::toResponse).toList());
    }

    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        return mapper.toResponse(get(id));
    }

    public UserResponse create(CreateUserRequest r) {
        if (users.existsByUsername(r.username()) || users.existsByEmail(r.email()))
            throw new ConflictException("Username or email already exists");
        var user = User.builder().fullName(r.fullName()).username(r.username()).email(r.email())
                .password(encoder.encode(r.password())).phone(r.phone())
                .role(r.role() != null ? r.role() : RoleName.CUSTOMER).build();
        return mapper.toResponse(users.save(user));
    }

    public UserResponse update(UUID id, UpdateUserRequest r) {
        var user = get(id);
        user.setFullName(r.fullName());
        user.setEmail(r.email());
        user.setPhone(r.phone());
        if (r.status() != null)
            user.setStatus(r.status());
        if (r.role() != null)
            user.setRole(r.role());
        return mapper.toResponse(user);
    }

    public void delete(UUID id) {
        var user = get(id);
        user.setStatus(UserStatus.DISABLED);
    }

    @Transactional(readOnly = true)
    public UserResponse profile() {
        return mapper.toResponse(current());
    }

    public UserResponse updateProfile(UpdateProfileRequest r) {
        var u = current();
        u.setFullName(r.fullName());
        u.setEmail(r.email());
        u.setPhone(r.phone());
        return mapper.toResponse(u);
    }

    private User get(UUID id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }

    private User current() {
        return users.findByUsername(SecurityUtils.currentUsername())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
