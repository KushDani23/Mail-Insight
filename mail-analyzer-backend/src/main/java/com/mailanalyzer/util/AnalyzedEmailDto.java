package com.mailanalyzer.util;

import com.mailanalyzer.enums.EmailCategory;
import com.mailanalyzer.enums.EmailPriority;
import lombok.Builder;
import lombok.Getter;

/**
 * Internal DTO that holds one validated result from the Gemini response.
 *
 * <p>Created by {@link GeminiResponseParser} after verifying that the
 * Gemini-returned JSON object is structurally valid.  Passed to
 * {@link com.mailanalyzer.service.EmailService} for persistence.
 *
 * <p>Like {@link GmailMessageDto}, this class is NOT a Spring bean and
 * exists only on the call stack during an analysis request.
 */
@Getter
@Builder
public class AnalyzedEmailDto {

    /** Matched back to the original GmailMessageDto.gmailMessageId. */
    private final String gmailMessageId;

    /** Gemini-generated 1-2 sentence summary. */
    private final String summary;

    /** Validated category – must be a member of {@link EmailCategory}. */
    private final EmailCategory category;

    /** Validated priority – must be 1–5 and match the category's expected level. */
    private final EmailPriority priority;
}
