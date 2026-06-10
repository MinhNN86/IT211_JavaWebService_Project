package com.project.modules.storage.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.project.common.response.ApiResponse;
import com.project.modules.storage.dto.response.FileUploadResponse;
import com.project.modules.storage.service.FileStorageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class FileUploadController {
    private final FileStorageService service;
    @PostMapping("/api/v1/files/upload")
    ApiResponse<FileUploadResponse> upload(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success("Upload successfully", service.storeCourtImage(file));
    }

    @PostMapping("/api/v1/manager/courts/{courtId}/images")
    ApiResponse<FileUploadResponse> attach(@PathVariable Long courtId, @RequestPart("file") MultipartFile file) {
        return ApiResponse.success("Upload successfully", service.attachToCourt(courtId, file));
    }
}
