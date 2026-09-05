package com.mailinsight.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiKeyRequest(
                @NotBlank(message = "API key must not be blank") @Size(min = 10, max = 256, message = "API key length must be between 10 and 256 characters") String apiKey) {
}
