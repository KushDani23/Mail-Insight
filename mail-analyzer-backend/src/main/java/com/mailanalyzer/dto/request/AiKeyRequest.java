package com.mailanalyzer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/user/ai-key.
 * Contains the raw Gemini API key to be encrypted and stored.
 *
 * <p>The raw key is processed in memory only and encrypted before persistence.
 * It is NEVER logged.
 */
public record AiKeyRequest(

        @NotBlank(message = "API key must not be blank")
        @Size(min = 10, max = 256, message = "API key length must be between 10 and 256 characters")
        String apiKey

) {}
