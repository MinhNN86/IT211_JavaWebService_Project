package com.project.modules.user.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import com.project.common.response.*;
import com.project.modules.user.dto.request.*;
import com.project.modules.user.dto.response.UserResponse;
import com.project.modules.user.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserService service;
    @GetMapping
    ApiResponse<PageResponse<UserResponse>> all(@RequestParam(required = false) String keyword, Pageable p) {
        return ApiResponse.success("Users retrieved", service.findAll(keyword, p));
    }

    @GetMapping("/{id}")
    ApiResponse<UserResponse> one(@PathVariable UUID id) {
        return ApiResponse.success("User retrieved", service.findById(id));
    }

    @PostMapping
    ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest r) {
        return ResponseEntity.status(201).body(ApiResponse.success("User created", service.create(r)));
    }

    @PutMapping("/{id}")
    ApiResponse<UserResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest r) {
        return ApiResponse.success("User updated", service.update(id, r));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
