package com.mailanalyzer.dto.response;

import com.mailanalyzer.enums.EmailCategory;
import com.mailanalyzer.enums.EmailPriority;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for an analyzed email.
 *
 * <p>Note: Only metadata and summary are included.
 * Raw email body is NEVER part of any response.
 */
public record EmailResponse(
        UUID id,
        String gmailMessageId,
        String sourceAccount,
        String sender,
        String subject,
        String summary,
        EmailCategory category,
        EmailPriority priority,
        Instant receivedAt,
        Instant analyzedAt
) {}
