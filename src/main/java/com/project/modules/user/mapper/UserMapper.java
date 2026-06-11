package com.project.modules.user.mapper;

import org.springframework.stereotype.Component;

import com.project.modules.user.dto.response.UserResponse;
import com.project.modules.user.entity.User;

@Component
public class UserMapper {
    public UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getUsername(), u.getEmail(), u.getPhone(), u.isActive(),
                u.getRole(), u.getCreatedAt());
    }
}
