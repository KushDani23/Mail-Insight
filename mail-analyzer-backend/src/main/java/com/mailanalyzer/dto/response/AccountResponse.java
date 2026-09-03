package com.mailanalyzer.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a connected Gmail account.
 * Returned by GET /api/accounts.
 * Access/refresh tokens are NEVER included in this DTO.
 */
public record AccountResponse(
        UUID id,
        String gmailAddress,
        Instant createdAt
) {}
