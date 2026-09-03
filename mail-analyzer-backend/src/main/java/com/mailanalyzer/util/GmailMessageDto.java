package com.mailanalyzer.util;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Internal DTO used to pass raw Gmail message data between
 * {@link com.mailanalyzer.service.GmailService} and
 * {@link com.mailanalyzer.service.EmailService} / {@link com.mailanalyzer.service.GeminiService}.
 *
 * <p><b>PRIVACY RULE:</b> This object is NEVER persisted.  The {@code body}
 * field is used only to build the Gemini prompt in memory.  Once Gemini
 * returns its analysis, the body is discarded and the summary is stored
 * in the {@link com.mailanalyzer.entity.Email} entity instead.
 *
 * <p>This class is intentionally NOT a Spring bean (no @Component).
 * It lives only on the call stack during an analysis request.
 */
@Getter
@Builder
public class GmailMessageDto {

    /** Gmail's stable, globally-unique message identifier. */
    private final String gmailMessageId;

    /** Which connected Gmail account received this message. */
    private final String sourceAccount;

    /** Sender name + address (From header). */
    private final String sender;

    /** Email subject line. */
    private final String subject;

    /**
     * Plain-text body content (or snippet if full body is unavailable).
     * Used ONLY for the Gemini analysis prompt; never stored.
     */
    private final String body;

    /** When the email was sent/received (UTC, parsed from Gmail headers). */
    private final Instant receivedAt;
}
