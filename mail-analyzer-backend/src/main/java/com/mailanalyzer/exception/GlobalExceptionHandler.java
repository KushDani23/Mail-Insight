package com.mailanalyzer.exception;

import com.mailanalyzer.dto.response.ApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * Central exception handler for all REST API errors.
 *
 * <p>All responses use the uniform {@link ApiError} shape:
 * <pre>
 * {
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "No Gemini API key found. Please save your key in Settings.",
 *   "timestamp": "2024-01-01T12:00:00Z"
 * }
 * </pre>
 *
 * <p><b>Security rule:</b> Stack traces, internal class names, and SQL errors
 * are NEVER included in API responses. Only the {@code message} field is
 * exposed, and it is always a human-readable string safe to show to the user.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── 404 Not Found ─────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, "Not Found", ex.getMessage()));
    }

    // ── 502 Bad Gateway (upstream failures) ───────────────────────────────

    @ExceptionHandler(GeminiException.class)
    public ResponseEntity<ApiError> handleGemini(GeminiException ex) {
        log.error("Gemini API error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ApiError.of(502, "Gemini Error", ex.getMessage()));
    }

    @ExceptionHandler(GmailException.class)
    public ResponseEntity<ApiError> handleGmail(GmailException ex) {
        log.error("Gmail API error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ApiError.of(502, "Gmail Error", ex.getMessage()));
    }

    // ── 400 Bad Request ───────────────────────────────────────────────────

    /**
     * Handles @Valid validation failures on request bodies.
     * Collects all field-level errors into a readable message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(400, "Bad Request", message));
    }

    /**
     * Handles invalid path variable types (e.g. passing "abc" for an int priority).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'";
        log.warn("Type mismatch: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(400, "Bad Request", message));
    }

    /**
     * Handles invalid enum values in path variables (e.g. unknown category name).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(400, "Bad Request", ex.getMessage()));
    }

    // ── 403 Forbidden ─────────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(403, "Forbidden", "You do not have permission to access this resource."));
    }

    // ── 500 Internal Server Error ─────────────────────────────────────────

    /**
     * Catch-all for any unhandled exception.
     * The real error is logged server-side; the client only sees a generic message.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(500, "Internal Server Error",
                        "An unexpected error occurred. Please try again later."));
    }
}
