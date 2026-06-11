package com.project.modules.storage.dto.response;

import java.util.UUID;

public record FileUploadResponse(
        UUID id,
        String fileName,
        String url) {
}
