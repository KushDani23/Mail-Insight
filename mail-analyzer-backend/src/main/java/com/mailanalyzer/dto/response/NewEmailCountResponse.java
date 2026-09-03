package com.mailanalyzer.dto.response;

/**
 * Response DTO for GET /api/emails/new-count.
 *
 * <p>The frontend uses {@code count} to decide whether to enable or
 * disable the "Analyze Emails" button.  If count == 0, the button is
 * disabled and no Gemini request will be made on click.
 */
public record NewEmailCountResponse(
        int count,
        boolean analyzeEnabled
) {
    /** Convenience factory: analyzeEnabled is true only when count > 0. */
    public static NewEmailCountResponse of(int count) {
        return new NewEmailCountResponse(count, count > 0);
    }
}
