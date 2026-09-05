package com.mailinsight.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AccountResponse(UUID id, String gmailAddress, Instant createdAt) {
}
