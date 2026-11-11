package com.nexus.rag;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SpringAiRagApplicationTests {

	@TestConfiguration
	static class TestConfig {

		@Bean
		@Primary
		public VectorStore vectorStore() {
			return Mockito.mock(VectorStore.class);
		}

		@Bean
		@Primary
		public ChatClient.Builder chatClientBuilder() {
			ChatClient mockChatClient = Mockito.mock(ChatClient.class);
			ChatClient.Builder builder = Mockito.mock(ChatClient.Builder.class, Mockito.RETURNS_DEEP_STUBS);
			Mockito.when(builder.build()).thenReturn(mockChatClient);
			return builder;
		}
	}

	@Test
	void contextLoads() {
	}

}
