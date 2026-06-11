package com.project.modules.court.dto.response;

import java.util.UUID;

public record CourtManagerResponse(
        UUID id,
        String username,
        String fullName) {
}
