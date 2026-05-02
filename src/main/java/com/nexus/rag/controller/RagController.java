package com.nexus.rag.controller;

import com.nexus.rag.dto.AskRequest;
import com.nexus.rag.dto.AskResponse;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for the RAG (Retrieval-Augmented Generation) query interface.
 */
@RestController
@RequestMapping("/api/v1/chat")
@Tag(name = "RAG Chat", description = "AI-powered question answering using your own ingested documents.")
@RateLimiter(name = "chatLimit")
public class RagController {

    private final ChatClient chatClient;

    @Value("${spring.ai.google.genai.chat.options.model:gemini-2.5-flash}")
    private String modelName;

    public RagController(ChatClient.Builder builder, VectorStore vectorStore, ChatMemory chatMemory) {
        this.chatClient = builder
                .defaultAdvisors(
                        // Add conversational memory
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // Add RAG context
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder().topK(5).build())
                                .build()
                )
                .build();
    }

    /**
     * Ask a question via a GET request with a query parameter.
     */
    @GetMapping("/ask")
    @Operation(
            summary = "Ask a question (GET)",
            description = "Convenience endpoint — pass your question as a query parameter. Uses RAG to retrieve relevant context and generate an AI-powered answer.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful response",
                            content = @Content(schema = @Schema(implementation = AskResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<AskResponse> askGet(
            @Parameter(description = "The question to ask", example = "How to build RAG with Spring AI and MongoDB?")
            @RequestParam(value = "message",
                    defaultValue = "How to analyze time-series data with Python and MongoDB?") String message,
            @Parameter(description = "Optional session ID for conversational memory")
            @RequestParam(value = "chatId", required = false) String chatId) {

        String effectiveChatId = (chatId != null && !chatId.isBlank()) ? chatId : UUID.randomUUID().toString();

        String answer = chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, effectiveChatId))
                .call()
                .content();

        return ResponseEntity.ok(AskResponse.of(message, answer, modelName));
    }

    /**
     * Ask a question via a POST request with a JSON body.
     */
    @PostMapping("/ask")
    @Operation(
            summary = "Ask a question (POST)",
            description = "Production endpoint — send a JSON body. Uses RAG to retrieve relevant context and generate an AI-powered answer. Supports conversational memory via chatId.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful response",
                            content = @Content(schema = @Schema(implementation = AskResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<AskResponse> askPost(@Valid @RequestBody AskRequest request) {
        String effectiveChatId = (request.chatId() != null && !request.chatId().isBlank())
                ? request.chatId() : UUID.randomUUID().toString();

        String answer = chatClient.prompt()
                .user(request.message())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, effectiveChatId))
                .call()
                .content();

        return ResponseEntity.ok(AskResponse.of(request.message(), answer, modelName));
    }

    /**
     * Backward-compatible endpoint matching the original /faq path.
     */
    @GetMapping("/faq")
    @Operation(summary = "Ask a question (legacy)", description = "Legacy compatibility endpoint. Prefer /api/v1/chat/ask.")
    public ResponseEntity<AskResponse> faqLegacy(
            @RequestParam(value = "message",
                    defaultValue = "How to analyze time-series data with Python and MongoDB?") String message) {
        return askGet(message, null);
    }
}
