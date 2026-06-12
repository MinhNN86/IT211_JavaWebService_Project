package com.project.modules.storage.service;

import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryStorageClient {
    UploadedAsset uploadImage(MultipartFile file, String publicId);

    void deleteImage(String publicId);

    record UploadedAsset(
            String publicId,
            String secureUrl) {
    }
}
