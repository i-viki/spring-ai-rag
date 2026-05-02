package com.nexus.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Spring AI RAG application.
 *
 * <p>This application demonstrates a production-ready Retrieval-Augmented Generation
 * (RAG) pipeline using Spring Boot, Spring AI, MongoDB Atlas Vector Search, and OpenAI.</p>
 */
@SpringBootApplication
public class SpringAiRagApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringAiRagApplication.class, args);
	}

}
