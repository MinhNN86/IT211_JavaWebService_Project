package com.project.modules.storage.service;

import org.springframework.web.multipart.MultipartFile;

import com.project.modules.storage.dto.response.FileUploadResponse;

public interface FileStorageService {
    FileUploadResponse storeCourtImage(MultipartFile file);

    FileUploadResponse attachToCourt(Long courtId, MultipartFile file);
}
