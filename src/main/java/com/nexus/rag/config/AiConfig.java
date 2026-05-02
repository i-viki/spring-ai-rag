package com.nexus.rag.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Spring AI components.
 */
@Configuration
public class AiConfig {

    /**
     * Define a ChatMemory bean to store conversation history.
     * MessageWindowChatMemory maintains a sliding window of the last N messages.
     */
    @Bean
    public ChatMemory chatMemory() {
        // Spring AI 1.1.5 enforces the use of the builder pattern for MessageWindowChatMemory
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }
}
