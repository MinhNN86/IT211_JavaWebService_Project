package com.project.modules.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cloudinary")
public record CloudinaryProperties(
        String cloudName,
        String apiKey,
        String apiSecret,
        String uploadFolder) {
    public String normalizedUploadFolder() {
        if (uploadFolder == null || uploadFolder.isBlank()) {
            return "courts";
        }
        return uploadFolder.strip().replaceAll("^/+|/+$", "");
    }
}
