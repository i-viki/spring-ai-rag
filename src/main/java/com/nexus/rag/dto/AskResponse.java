package com.nexus.rag.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Response DTO for the /api/v1/chat/ask endpoint.
 *
 * @param question  The original user question.
 * @param answer    The AI-generated answer augmented with retrieved context.
 * @param model     The AI model used for generation.
 * @param timestamp The server timestamp when the response was created.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AskResponse(
        String question,
        String answer,
        String model,
        Instant timestamp
) {

    /**
     * Convenience factory method.
     */
    public static AskResponse of(String question, String answer, String model) {
        return new AskResponse(question, answer, model, Instant.now());
    }
}
