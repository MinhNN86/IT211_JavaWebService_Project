package com.project.modules.storage.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
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

    @PostMapping("/api/v1/manager/courts/{courtId}/images")
    ApiResponse<List<FileUploadResponse>> attach(@PathVariable Long courtId,
            @RequestPart("files") List<MultipartFile> files) {
        return ApiResponse.success("Upload successfully", service.attachToCourt(courtId, files));
    }

    @DeleteMapping("/api/v1/manager/courts/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteCourtImage(@PathVariable UUID imageId) {
        service.deleteCourtImage(imageId);
    }
}
