package com.nexus.rag.dto;

import java.time.Instant;

/**
 * Response DTO for the document ingestion endpoint.
 *
 * @param status         The ingestion result status.
 * @param documentsCount Number of document chunks ingested into the vector store.
 * @param durationMs     Time taken for the ingestion process in milliseconds.
 * @param timestamp      The server timestamp when ingestion completed.
 */
public record IngestionResponse(
        String status,
        int documentsCount,
        long durationMs,
        Instant timestamp
) {

    public static IngestionResponse success(int documentsCount, long durationMs) {
        return new IngestionResponse("SUCCESS", documentsCount, durationMs, Instant.now());
    }

    public static IngestionResponse failure(String errorMessage) {
        return new IngestionResponse("FAILED: " + errorMessage, 0, 0, Instant.now());
    }
}
