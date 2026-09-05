package com.mailinsight.dto.response;

import com.mailinsight.enums.EmailCategory;
import com.mailinsight.enums.EmailPriority;

import java.time.Instant;
import java.util.UUID;

public record EmailResponse(UUID id, String gmailMessageId, String sourceAccount, String sender, String subject,
                String summary, EmailCategory category, EmailPriority priority, Instant receivedAt,
                Instant analyzedAt) {
}
