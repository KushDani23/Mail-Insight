package com.mailanalyzer.dto.response;

/**
 * Response DTO for POST /api/emails/analyze.
 * Tells the frontend how many emails were successfully analyzed and stored.
 */
public record AnalyzeResultResponse(
        int analyzed,
        int skipped,     // emails Gemini returned that failed validation
        String message
) {}
