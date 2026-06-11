package com.project.modules.auth.dto.response;

public record RefreshResponse(
        String accessToken,
        String tokenType) {
}
