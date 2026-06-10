package com.project.modules.storage.service;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.project.modules.storage.dto.response.FileUploadResponse;

public interface FileStorageService {
    FileUploadResponse storeCourtImage(MultipartFile file);

    List<FileUploadResponse> attachToCourt(Long courtId, List<MultipartFile> files);

    void deleteCourtImage(UUID imageId);
}
