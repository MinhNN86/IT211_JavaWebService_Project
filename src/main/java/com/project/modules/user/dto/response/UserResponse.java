package com.project.modules.user.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.project.common.enums.*;

public record UserResponse(UUID id, String fullName, String username, String email, String phone, UserStatus status,
        RoleName role, LocalDateTime createdAt) {
}
