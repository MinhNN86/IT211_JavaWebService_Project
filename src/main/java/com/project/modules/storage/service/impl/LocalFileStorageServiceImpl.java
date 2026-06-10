package com.project.modules.storage.service.impl;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.project.common.exception.*;
import com.project.modules.court.entity.CourtImage;
import com.project.modules.court.repository.CourtImageRepository;
import com.project.modules.court.repository.CourtRepository;
import com.project.modules.court.service.CourtAccessService;
import com.project.modules.storage.dto.response.FileUploadResponse;
import com.project.modules.storage.service.FileStorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocalFileStorageServiceImpl implements FileStorageService {
    private static final Map<String, String> TYPES = Map.of("image/png", ".png", "image/jpeg", ".jpg", "image/jpg",
            ".jpg", "image/webp", ".webp");
    private final CourtRepository courts;
    private final CourtImageRepository images;
    private final CourtAccessService courtAccess;
    @Value("${app.file.upload-dir}")
    private String uploadDir;
    @Value("${app.file.public-path}")
    private String publicPath;
    public FileUploadResponse storeCourtImage(MultipartFile file) {
        validateImage(file);
        UUID id = UUID.randomUUID();
        String name = id + TYPES.get(file.getContentType());
        Path directory = Paths.get(uploadDir, "courts").toAbsolutePath().normalize();
        try {
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), directory.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BadRequestException("Could not store image");
        }
        return new FileUploadResponse(id, name, publicPath + "/courts/" + name);
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new BadRequestException("Image file is required");
        if (file.getSize() > 10 * 1024 * 1024)
            throw new BadRequestException("Image size must not exceed 10MB");
        if (file.getContentType() == null || !TYPES.containsKey(file.getContentType()))
            throw new BadRequestException("Only PNG, JPG, JPEG and WEBP images are allowed");
    }

    @Transactional
    public List<FileUploadResponse> attachToCourt(Long courtId, List<MultipartFile> files) {
        courtAccess.requireCanManage(courtId);
        var court = courts.findById(courtId).orElseThrow(() -> new NotFoundException("Court not found"));
        if (files == null || files.isEmpty())
            throw new BadRequestException("At least one image file is required");
        files.forEach(this::validateImage);

        List<FileUploadResponse> storedFiles = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                var stored = storeCourtImage(file);
                storedFiles.add(stored);
                images.save(
                        CourtImage.builder().id(stored.id()).court(court).fileName(stored.fileName()).url(stored.url())
                                .build());
            }
        } catch (RuntimeException ex) {
            storedFiles.forEach(stored -> deleteStoredFileQuietly(stored.fileName()));
            throw ex;
        }
        return storedFiles;
    }

    @Override
    @Transactional
    public void deleteCourtImage(UUID imageId) {
        var image = images.findById(imageId).orElseThrow(() -> new NotFoundException("Court image not found"));
        courtAccess.requireCanManage(image.getCourt().getId());
        deleteStoredFile(image.getFileName());
        images.delete(image);
    }

    private void deleteStoredFile(String fileName) {
        Path directory = Paths.get(uploadDir, "courts").toAbsolutePath().normalize();
        Path imagePath = directory.resolve(fileName).normalize();
        if (!imagePath.startsWith(directory))
            throw new BadRequestException("Invalid image path");
        try {
            Files.deleteIfExists(imagePath);
        } catch (IOException ex) {
            throw new BadRequestException("Could not delete image");
        }
    }

    private void deleteStoredFileQuietly(String fileName) {
        try {
            deleteStoredFile(fileName);
        } catch (BadRequestException ignored) {
            // Preserve the original upload error while attempting best-effort cleanup.
        }
    }
}
