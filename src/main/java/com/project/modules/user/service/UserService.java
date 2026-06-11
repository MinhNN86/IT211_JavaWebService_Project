package com.project.modules.user.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.project.common.response.PageResponse;
import com.project.modules.user.dto.request.*;
import com.project.modules.user.dto.response.UserResponse;

public interface UserService {
    PageResponse<UserResponse> findAll(String keyword, Pageable pageable);

    UserResponse findById(UUID id);

    UserResponse create(CreateUserRequest request);

    UserResponse update(UUID id, UpdateUserRequest request);

    UserResponse disable(UUID id);

    void delete(UUID id);

    UserResponse profile();

    UserResponse updateProfile(UpdateProfileRequest request);
}
