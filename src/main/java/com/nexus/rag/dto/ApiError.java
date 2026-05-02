package com.nexus.rag.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Standardized error response body for all API errors.
 *
 * @param status     HTTP status code.
 * @param error      HTTP status reason phrase.
 * @param message    A human-readable summary of the error.
 * @param details    Optional list of field-level validation errors.
 * @param path       The request URI that caused the error.
 * @param timestamp  The server timestamp when the error occurred.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        int status,
        String error,
        String message,
        List<String> details,
        String path,
        Instant timestamp
) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(status, error, message, null, path, Instant.now());
    }

    public static ApiError of(int status, String error, String message, List<String> details, String path) {
        return new ApiError(status, error, message, details, path, Instant.now());
    }
}
