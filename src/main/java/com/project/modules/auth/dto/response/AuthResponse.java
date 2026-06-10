package com.project.modules.auth.dto.response;

public record AuthResponse(String accessToken, String refreshToken, String tokenType) {
}
