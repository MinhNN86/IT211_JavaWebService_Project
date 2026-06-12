package com.project.modules.storage.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.project.common.exception.BadRequestException;
import com.project.common.exception.NotFoundException;
import com.project.modules.court.entity.Court;
import com.project.modules.court.entity.CourtImage;
import com.project.modules.court.repository.CourtImageRepository;
import com.project.modules.court.repository.CourtRepository;
import com.project.modules.court.service.CourtAccessService;
import com.project.modules.storage.config.CloudinaryProperties;
import com.project.modules.storage.dto.response.FileUploadResponse;
import com.project.modules.storage.service.CloudinaryStorageClient;
import com.project.modules.storage.service.FileStorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CloudinaryFileStorageServiceImpl implements FileStorageService {
    private static final long MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024;
    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of("image/png", "png", "image/jpeg", "jpg",
            "image/jpg", "jpg", "image/webp", "webp");

    private final CourtRepository courtRepository;
    private final CourtImageRepository courtImageRepository;
    private final CourtAccessService courtAccessService;
    private final CloudinaryStorageClient cloudinaryStorageClient;
    private final CloudinaryProperties cloudinaryProperties;

    @Override
    public FileUploadResponse storeCourtImage(MultipartFile file) {
        validateImage(file);

        UUID imageId = UUID.randomUUID();
        String imageKey = imageId.toString();
        String publicId = toCloudinaryPublicId(imageKey);
        CloudinaryStorageClient.UploadedAsset uploadedAsset = cloudinaryStorageClient.uploadImage(file, publicId);

        return new FileUploadResponse(imageId, imageKey, uploadedAsset.secureUrl());
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new BadRequestException("Image size must not exceed 10MB");
        }
        if (file.getContentType() == null || !ALLOWED_IMAGE_TYPES.containsKey(file.getContentType())) {
            throw new BadRequestException("Only PNG, JPG, JPEG and WEBP images are allowed");
        }
    }

    @Override
    @Transactional
    public List<FileUploadResponse> attachToCourt(Long courtId, List<MultipartFile> files) {
        courtAccessService.requireCanManage(courtId);
        Court court = courtRepository.findById(courtId).orElseThrow(() -> new NotFoundException("Court not found"));

        if (files == null || files.isEmpty()) {
            throw new BadRequestException("At least one image file is required");
        }
        files.forEach(this::validateImage);

        List<FileUploadResponse> storedFiles = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                FileUploadResponse storedFile = storeCourtImage(file);
                storedFiles.add(storedFile);

                CourtImage courtImage = CourtImage.builder()
                        .id(storedFile.id())
                        .court(court)
                        .fileName(storedFile.fileName())
                        .url(storedFile.url())
                        .build();
                courtImageRepository.save(courtImage);
            }
        } catch (RuntimeException exception) {
            storedFiles.forEach(stored -> deleteStoredFileQuietly(stored.fileName()));
            throw exception;
        }

        return storedFiles;
    }

    @Override
    @Transactional
    public void deleteCourtImage(UUID imageId) {
        CourtImage courtImage = courtImageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("Court image not found"));

        courtAccessService.requireCanManage(courtImage.getCourt().getId());
        cloudinaryStorageClient.deleteImage(toCloudinaryPublicId(courtImage.getFileName()));
        courtImageRepository.delete(courtImage);
    }

    private void deleteStoredFileQuietly(String imageKey) {
        try {
            cloudinaryStorageClient.deleteImage(toCloudinaryPublicId(imageKey));
        } catch (BadRequestException ignored) {
            // Preserve the original upload error while attempting best-effort cleanup.
        }
    }

    private String toCloudinaryPublicId(String imageKey) {
        if (imageKey.contains("/")) {
            return imageKey;
        }
        return cloudinaryProperties.normalizedUploadFolder() + "/" + imageKey;
    }
}
