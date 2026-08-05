package com.redclubes.backend.config;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors,
        String path
) {
    public static ApiErrorResponse of(
            int status,
            String error,
            String message,
            String path
    ) {
        return new ApiErrorResponse(Instant.now(), status, error, message, Map.of(), path);
    }

    public static ApiErrorResponse validation(
            int status,
            String message,
            Map<String, String> fieldErrors,
            String path
    ) {
        return new ApiErrorResponse(Instant.now(), status, "VALIDATION_ERROR", message, fieldErrors, path);
    }
}
