package com.project.modules.storage.service.impl;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.project.common.exception.*;
import com.project.modules.court.repository.CourtRepository;
import com.project.modules.storage.dto.response.FileUploadResponse;
import com.project.modules.storage.service.FileStorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocalFileStorageServiceImpl implements FileStorageService {
    private static final Map<String, String> TYPES = Map.of("image/png", ".png", "image/jpeg", ".jpg", "image/jpg",
            ".jpg", "image/webp", ".webp");
    private final CourtRepository courts;
    @Value("${app.file.upload-dir}")
    private String uploadDir;
    @Value("${app.file.public-path}")
    private String publicPath;
    public FileUploadResponse storeCourtImage(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new BadRequestException("Image file is required");
        if (file.getSize() > 10 * 1024 * 1024)
            throw new BadRequestException("Image size must not exceed 10MB");
        if (!TYPES.containsKey(file.getContentType()))
            throw new BadRequestException("Only PNG, JPG, JPEG and WEBP images are allowed");
        String name = UUID.randomUUID() + TYPES.get(file.getContentType());
        Path directory = Paths.get(uploadDir, "courts").toAbsolutePath().normalize();
        try {
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), directory.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BadRequestException("Could not store image");
        }
        return new FileUploadResponse(name, publicPath + "/courts/" + name);
    }

    @Transactional
    public FileUploadResponse attachToCourt(Long courtId, MultipartFile file) {
        var court = courts.findById(courtId).orElseThrow(() -> new NotFoundException("Court not found"));
        var stored = storeCourtImage(file);
        court.setImageUrl(stored.url());
        return stored;
    }
}
