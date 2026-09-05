package com.mailinsight.dto.response;

import java.time.Instant;
import java.util.Map;

public record ApiError(int status, String error, String message, Instant timestamp, Map<String, Object> extra) {
    public static ApiError of(int status, String error, String message) {
        return new ApiError(status, error, message, Instant.now(), null);
    }

    public static ApiError of(int status, String error, String message, Map<String, Object> extra) {
        return new ApiError(status, error, message, Instant.now(), extra);
    }
}
