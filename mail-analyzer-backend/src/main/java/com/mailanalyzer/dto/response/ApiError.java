package com.mailanalyzer.dto.response;

import java.time.Instant;

/**
 * Uniform error response body returned by
 * {@link com.mailanalyzer.exception.GlobalExceptionHandler}.
 *
 * <p>Internal stack traces and exception class names are NEVER included.
 */
public record ApiError(
        int status,
        String error,     // short error type, e.g. "Not Found"
        String message,   // human-readable description
        Instant timestamp
) {
    public static ApiError of(int status, String error, String message) {
        return new ApiError(status, error, message, Instant.now());
    }
}
