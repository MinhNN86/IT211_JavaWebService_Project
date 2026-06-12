package com.project.modules.storage.service.impl;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.project.common.exception.BadRequestException;
import com.project.common.exception.ExternalStorageException;
import com.project.modules.storage.service.CloudinaryStorageClient;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CloudinaryStorageClientImpl implements CloudinaryStorageClient {
    private final Cloudinary cloudinary;

    @Override
    public UploadedAsset uploadImage(MultipartFile file, String publicId) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "resource_type", "image",
                    "public_id", publicId,
                    "overwrite", false));

            Object uploadedPublicId = result.get("public_id");
            Object secureUrl = result.get("secure_url");
            if (uploadedPublicId == null || secureUrl == null) {
                throw new BadRequestException("Cloudinary did not return image details");
            }

            return new UploadedAsset(uploadedPublicId.toString(), secureUrl.toString());
        } catch (BadRequestException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new ExternalStorageException(
                    "Could not upload image to Cloudinary. Check Cloudinary API key permissions and credentials.",
                    exception);
        }
    }

    @Override
    public void deleteImage(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", "image",
                    "invalidate", true));
        } catch (IOException | RuntimeException exception) {
            throw new ExternalStorageException(
                    "Could not delete image from Cloudinary. Check Cloudinary API key permissions and credentials.",
                    exception);
        }
    }
}
