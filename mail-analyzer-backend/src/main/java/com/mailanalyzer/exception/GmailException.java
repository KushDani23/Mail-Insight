package com.mailanalyzer.exception;

/**
 * Thrown when the Gmail API returns an error or when token refresh fails.
 *
 * <p>Handled by {@link GlobalExceptionHandler} as HTTP 502 Bad Gateway.
 */
public class GmailException extends RuntimeException {

    public GmailException(String message) {
        super(message);
    }

    public GmailException(String message, Throwable cause) {
        super(message, cause);
    }
}
