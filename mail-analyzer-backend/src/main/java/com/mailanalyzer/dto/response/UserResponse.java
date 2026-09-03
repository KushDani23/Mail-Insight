package com.mailanalyzer.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for the currently authenticated user.
 * Returned by GET /api/auth/me.
 */
public record UserResponse(
        UUID id,
        String email,
        String name,
        String pictureUrl,
        Instant createdAt
) {}
