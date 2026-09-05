package com.mailinsight.exception;

import com.mailinsight.dto.response.ApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        // Error 404 not found
        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
                log.warn("Resource not found: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(ApiError.of(404, "Not Found", ex.getMessage()));
        }

        // error 502 bad gateway
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

        // Error 422 Unprocessable Entity
        @ExceptionHandler(InsufficientEmailsException.class)
        public ResponseEntity<ApiError> handleInsufficientEmails(InsufficientEmailsException ex) {
                log.info("Analysis blocked: {}/{} emails", ex.getCurrentCount(), ex.getRequiredCount());
                return ResponseEntity
                                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                                .body(ApiError.of(422, "Insufficient Emails", ex.getMessage(),
                                                Map.of("currentCount", ex.getCurrentCount(),
                                                                "requiredCount", ex.getRequiredCount())));
        }

        // Error 400 Bad Request
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

        // invalid path variables types
        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
                String message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'";
                log.warn("Type mismatch: {}", message);
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiError.of(400, "Bad Request", message));
        }

        // invalid enum values
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
                log.warn("Illegal argument: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiError.of(400, "Bad Request", ex.getMessage()));
        }

        // Error 403 Forbidden
        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
                log.warn("Access denied: {}", ex.getMessage());
                return ResponseEntity
                                .status(HttpStatus.FORBIDDEN)
                                .body(ApiError.of(403, "Forbidden",
                                                "You do not have permission to access this resource."));
        }

        // Error 500 Internal Server Error
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiError> handleGeneric(Exception ex) {
                log.error("Unhandled exception: {}", ex.getMessage(), ex);
                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiError.of(500, "Internal Server Error",
                                                "An unexpected error occurred. Please try again later."));
        }
}
