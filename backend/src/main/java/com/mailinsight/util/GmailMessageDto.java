package com.mailinsight.util;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class GmailMessageDto {

    private final String gmailMessageId;
    private final String sourceAccount;
    private final String sender;
    private final String subject;
    private final String body;
    private final Instant receivedAt;
}
