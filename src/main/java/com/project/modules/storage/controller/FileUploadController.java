package com.project.modules.storage.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.project.common.response.ApiResponse;
import com.project.common.util.PublicUrlResolver;
import com.project.modules.storage.dto.response.FileUploadResponse;
import com.project.modules.storage.service.FileStorageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class FileUploadController {
    private final FileStorageService service;
    private final PublicUrlResolver publicUrlResolver;

    @PostMapping("/api/v1/manager/courts/{courtId}/images")
    ApiResponse<List<FileUploadResponse>> attach(@PathVariable Long courtId,
            @RequestPart("files") List<MultipartFile> files) {
        List<FileUploadResponse> uploadedFiles = service.attachToCourt(courtId, files).stream()
                .map(file -> new FileUploadResponse(file.id(), file.fileName(), publicUrlResolver.resolve(file.url())))
                .toList();
        return ApiResponse.success("Upload successfully", uploadedFiles);
    }

    @DeleteMapping("/api/v1/manager/courts/images/{imageId}")
    ApiResponse<Void> deleteCourtImage(@PathVariable UUID imageId) {
        service.deleteCourtImage(imageId);
        return ApiResponse.success("Delete successfully", null);
    }
}
