package com.nexus.rag.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger UI configuration.
 *
 * <p>Customizes the generated API documentation with project metadata,
 * licensing, and server information.</p>
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "ApiKeyAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Spring AI RAG API")
                        .version("1.0.0")
                        .description("""
                                Production-ready Retrieval-Augmented Generation (RAG) API.
                                
                                This API allows you to:
                                - **Ingest documents** into a MongoDB Atlas Vector Store
                                - **Ask questions** that are answered using your own data via Google Gemini
                                
                                Powered by Spring AI, MongoDB Atlas Vector Search, and Google Gemini.
                                """)
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0"))
                        .contact(new Contact()
                                .name("Nexus")
                                .url("https://github.com/i-viki/spring-ai-rag")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server")));
    }
}
