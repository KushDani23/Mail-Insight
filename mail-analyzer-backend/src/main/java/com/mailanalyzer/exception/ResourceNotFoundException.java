package com.mailanalyzer.exception;

/**
 * Thrown when a requested resource is not found in the database.
 *
 * <p>Examples:
 * <ul>
 *   <li>User has not saved a Gemini API key yet</li>
 *   <li>Connected account ID does not exist</li>
 * </ul>
 *
 * <p>Handled by {@link GlobalExceptionHandler} as HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
