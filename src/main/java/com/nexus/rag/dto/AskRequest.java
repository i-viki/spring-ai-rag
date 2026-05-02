package com.nexus.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for the /api/v1/chat/ask endpoint.
 *
 * @param message The user's question to be answered via RAG.
 * @param chatId  Optional session ID for conversational memory.
 */
public record AskRequest(
        @NotBlank(message = "Message must not be blank")
        @Size(min = 3, max = 2000, message = "Message must be between 3 and 2000 characters")
        @Schema(description = "The question to ask the AI", example = "How to build a RAG application?")
        String message,

        @Schema(description = "Optional session ID for conversational memory. If provided, the AI will remember previous questions in this session.", example = "user-123")
        String chatId
) {
}
