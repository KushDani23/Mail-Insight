package com.mailanalyzer.exception;

/**
 * Thrown when the Gemini API returns an error, empty response,
 * malformed JSON, or completely invalid analysis results.
 *
 * <p>Handled by {@link GlobalExceptionHandler} as HTTP 502 Bad Gateway,
 * so the frontend knows the failure was upstream (Gemini), not our server.
 */
public class GeminiException extends RuntimeException {

    public GeminiException(String message) {
        super(message);
    }

    public GeminiException(String message, Throwable cause) {
        super(message, cause);
    }
}
