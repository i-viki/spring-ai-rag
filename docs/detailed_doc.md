# Spring AI RAG — Detailed Technical Documentation

> **Version**: 1.0.0 · **Last Updated**: May 2026 · **License**: Apache 2.0

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [Required Software & Versions](#3-required-software--versions)
4. [Project Structure](#4-project-structure)
5. [Setup Guide](#5-setup-guide)
6. [Running the Application](#6-running-the-application)
7. [API Reference](#7-api-reference)
8. [Configuration Reference](#8-configuration-reference)
9. [Core Components Deep-Dive](#9-core-components-deep-dive)
10. [Docker Deployment](#10-docker-deployment)
11. [Monitoring & Health Checks](#11-monitoring--health-checks)
12. [Troubleshooting](#12-troubleshooting)
13. [Future Extensibility](#13-future-extensibility)

---

## 1. Project Overview

**Spring AI RAG** is a production-ready Java backend that implements the **Retrieval-Augmented Generation (RAG)** pattern. It bridges a Large Language Model (Google Gemini 2.5 Flash) with a private knowledge base stored in MongoDB Atlas, enabling AI-powered question answering over your own documents.

### What is RAG?

RAG solves a fundamental LLM limitation: models only know what they were trained on. By first *retrieving* relevant documents from a vector database and then *augmenting* the LLM prompt with that context, the model generates accurate, grounded answers based on **your** data — not just its training data.

### Key Capabilities

| Capability | Description |
|---|---|
| **Document Ingestion** | ETL pipeline that reads JSON, chunks with token-aware splitting, and stores vector embeddings |
| **Vector Search** | MongoDB Atlas Vector Search for high-performance similarity queries |
| **AI Chat** | Google Gemini integration via Spring AI's `ChatClient` with declarative RAG advisors |
| **Google AI Tier** | Uses Gemini 2.5 Flash / text-embedding-004 — **no paid API key required** |
| **Enterprise Features** | API Security (X-API-KEY), Rate Limiting, Conversational Memory, Prometheus Monitoring |
| **In-Memory History** | Conversation history is maintained for context-aware multi-turn chat |
| **API Documentation** | Auto-generated Swagger UI (OpenAPI 3.0) |
| **Health Monitoring** | Spring Boot Actuator endpoints for health, info, and metrics |
| **Docker Ready** | Multi-stage Dockerfile + Docker Compose |

---

## 2. Architecture

### High-Level Flow

```
┌──────────────────────────────────────────────────────────────────────┐
│                        CLIENT (Browser / cURL)                       │
└───────────┬──────────────────────────────────────────────┬───────────┘
            │ POST /api/v1/docs/load                       │ GET /api/v1/chat/ask?message=...
            ▼                                              ▼
┌───────────────────────┐                    ┌───────────────────────┐
│  DocsLoaderController │                    │     RagController     │
│  ─────────────────────│                    │  ─────────────────────│
│  Triggers ETL Pipeline│                    │  Handles Chat Queries │
└───────────┬───────────┘                    └───────────┬───────────┘
            │                                            │
            ▼                                            ▼
┌───────────────────────┐                    ┌───────────────────────┐
│   DocsLoaderService   │                    │  QuestionAnswerAdvisor│
│  ─────────────────────│                    │  (Spring AI built-in) │
│  1. Read JSON         │                    │  1. Embed user query  │
│  2. Extract metadata  │                    │  2. Vector search     │
│  3. Chunk via tokens  │                    │  3. Augment prompt    │
│  4. Batch ingest      │                    │  4. Call Gemini       │
└───────────┬───────────┘                    └───────────┬───────────┘
            │                                            │
            ▼                                            ▼
┌──────────────────────────────────────────────────────────────────────┐
│                      MongoDB Atlas Vector Store                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                  │
│  │  Document 1  │  │  Document 2  │  │  Document N  │                │
│  │  + Metadata  │  │  + Metadata  │  │  + Metadata  │                │
│  │  + Embedding │  │  + Embedding │  │  + Embedding │                │
│  └─────────────┘  └─────────────┘  └─────────────┘                  │
└──────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌───────────────────┐
                    │  Google Gemini    │
                    │  (2.0 Flash)     │
                    │  Embeddings +     │
                    │  Chat Completion  │
                    └───────────────────┘
```

### Technology Stack

| Layer | Technology | Version |
|---|---|---|
| **Runtime** | Java (Eclipse Temurin) | 21 (LTS) |
| **Framework** | Spring Boot | 3.5.5 |
| **AI Orchestration** | Spring AI | 1.1.5 |
| **Vector Database** | MongoDB Atlas | 7.0+ |
| **LLM Provider** | Google Gemini | 2.5 Flash |
| **Build Tool** | Apache Maven | 3.9+ |
| **API Docs** | SpringDoc OpenAPI | 2.8.5 |
| **Conversational Memory** | Spring AI MessageWindowChatMemory | In-memory, 20-msg window |
| **Security** | Spring Security + X-API-KEY filter | — |
| **Rate Limiting** | Resilience4j | 2.2.0 |
| **Observability** | Micrometer + Prometheus | — |
| **Containerization** | Docker | 24+ |

---

## 3. Required Software & Versions

### Mandatory

| Software | Minimum Version | Recommended | Download Link |
|---|---|---|---|
| **Java JDK** | 21 | 21 (LTS) | [Eclipse Temurin](https://adoptium.net/temurin/releases/) |
| **Apache Maven** | 3.9.0 | 3.9.9 | Included via `mvnw` wrapper — no install needed |
| **Git** | 2.30+ | Latest | [git-scm.com](https://git-scm.com/downloads) |

### External Services (Required)

| Service | Details | Signup |
|---|---|---|
| **Google Gemini API Key** | **FREE tier** — used for both embeddings and chat completion (Gemini 2.0 Flash) | [aistudio.google.com/apikey](https://aistudio.google.com/apikey) |
| **MongoDB Atlas** | Free tier (M0) works for development. M10+ recommended for production Vector Search | [cloud.mongodb.com](https://cloud.mongodb.com) |

### Optional (For Docker Deployment)

| Software | Minimum Version | Download Link |
|---|---|---|
| **Docker** | 24.0+ | [docker.com](https://www.docker.com/get-started/) |
| **Docker Compose** | 2.20+ | Bundled with Docker Desktop |

### Verifying Installation

```powershell
# Check Java
java -version
# Expected: openjdk version "21.x.x"

# Check Maven (via wrapper — no global install needed)
.\mvnw.cmd --version
# Expected: Apache Maven 3.9.x

# Check Docker (optional)
docker --version
# Expected: Docker version 24.x+

# Check Git
git --version
# Expected: git version 2.x+
```

---

## 4. Project Structure

```
spring-ai-rag/
├── .env.example                         # Environment variable template
├── .gitignore                           # Git ignore rules
├── Dockerfile                           # Multi-stage Docker build
├── docker-compose.yml                   # Docker Compose orchestration
├── LICENSE                              # Apache License 2.0
├── README.md                            # Project overview
├── mvnw / mvnw.cmd                      # Maven Wrapper (no Maven install needed)
├── pom.xml                              # Maven project configuration
│
├── docs/
│   └── detailed_doc.md                  # ← This document
│
├── src/
│   ├── main/
│   │   ├── java/com/nexus/rag/
│   │   │   ├── SpringAiRagApplication.java          # Spring Boot entry point
│   │   │   ├── config/
│   │   │   │   ├── AiConfig.java                   # ChatMemory bean (conversational history)
│   │   │   │   ├── OpenApiConfig.java               # Swagger/OpenAPI metadata + API key scheme
│   │   │   │   ├── SecurityConfig.java              # X-API-KEY filter + Spring Security
│   │   │   │   └── WebConfig.java                   # Global CORS policy
│   │   │   ├── controller/
│   │   │   │   ├── DocsLoaderController.java        # Document ingestion endpoints
│   │   │   │   └── RagController.java               # RAG chat/query endpoints
│   │   │   ├── dto/
│   │   │   │   ├── ApiError.java                    # Standardized error response
│   │   │   │   ├── AskRequest.java                  # Chat request DTO (POST)
│   │   │   │   ├── AskResponse.java                 # Chat response DTO
│   │   │   │   └── IngestionResponse.java           # Ingestion result DTO
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java      # @ControllerAdvice error handler
│   │   │   │   └── IngestionException.java          # Custom ingestion exception
│   │   │   └── service/
│   │   │       └── DocsLoaderService.java           # ETL pipeline (JSON + PDF → Vector Store, delete)
│   │   │
│   │   └── resources/
│   │       ├── application.yml                      # Main configuration
│   │       ├── application-test.yml                 # Test profile overrides
│   │       └── docs/
│   │           └── devcenter-content-snapshot.2024-05-20.json  # Sample knowledge base
│   │
│   └── test/
│       └── java/com/nexus/rag/
│           └── SpringAiRagApplicationTests.java     # Smoke test
│
└── target/                              # Build output (git-ignored)
```

### Package Naming Convention

This project uses `com.nexus.rag` as the base package:

- **`com.nexus`** — Organization/developer namespace (replaces the generic `com.example`)
- **`.rag`** — Module identifier for Retrieval-Augmented Generation

---

## 5. Setup Guide

### Step 1: Clone the Repository

```bash
git clone https://github.com/i-viki/spring-ai-rag.git
cd spring-ai-rag
```

### Step 2: Set Up MongoDB Atlas

1. Go to [cloud.mongodb.com](https://cloud.mongodb.com) and create a free account.
2. Create a new **Cluster** (M0 free tier works for development).
3. Under **Database Access**, create a database user with read/write permissions.
4. Under **Network Access**, add your IP address (or `0.0.0.0/0` for development).
5. Click **Connect** → **Drivers** and copy the connection string:
   ```
   mongodb+srv://<username>:<password>@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority
   ```

> **Note**: The application will automatically create the `rag` database and `vector_store` collection with a vector search index on first run (via `initialize-schema: true`).

### Step 3: Get a Gemini API Key (FREE)

1. Go to [aistudio.google.com/apikey](https://aistudio.google.com/apikey).
2. Sign in with your Google account.
3. Click **"Create API key"** and copy it.

> **💡 Tip**: The Gemini free tier includes generous rate limits for development and prototyping. No billing setup required.

### Step 4: Configure Environment Variables

**Option A — Using a `.env` file (Recommended)**:

```bash
# Copy the template
cp .env.example .env

# Edit with your values
# On Windows: notepad .env
# On macOS/Linux: nano .env
```

Fill in:
```properties
GEMINI_API_KEY=your-actual-gemini-key-here
MONGODB_URI=mongodb+srv://user:pass@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority
```

**Option B — Using system environment variables**:

```powershell
# Windows (PowerShell)
$env:GEMINI_API_KEY = "your-actual-gemini-key-here"
$env:MONGODB_URI = "mongodb+srv://user:pass@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority"
```

```bash
# macOS / Linux
export GEMINI_API_KEY=your-actual-gemini-key-here
export MONGODB_URI=mongodb+srv://user:pass@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority
```

---

## 6. Running the Application

### Option 1: Maven (Development)

```bash
# Using the Maven Wrapper (no Maven installation needed)
./mvnw spring-boot:run

# On Windows
.\mvnw.cmd spring-boot:run
```

The application starts on `http://localhost:8080`.

### Option 2: Build & Run JAR

```bash
# Build the fat JAR
./mvnw clean package -DskipTests

# Run it
java -jar target/spring-ai-rag-1.0.0.jar
```

### Option 3: Docker

```bash
# Using Docker Compose (reads .env automatically)
docker compose up --build

# Or build and run manually
docker build -t spring-ai-rag .
docker run -p 8080:8080 \
  -e GEMINI_API_KEY=your-key \
  -e MONGODB_URI=mongodb+srv://... \
  spring-ai-rag
```

### First Run: Ingest Documents

After the application starts, you must ingest the sample documents before asking questions:

```bash
# Trigger document ingestion (X-API-KEY required)
curl -X POST http://localhost:8080/api/v1/docs/load \
  -H "X-API-KEY: nexus-admin-key-2024"

# Or via GET (browser-friendly)
curl http://localhost:8080/api/v1/docs/load \
  -H "X-API-KEY: nexus-admin-key-2024"
```

Expected response:
```json
{
  "status": "SUCCESS",
  "documentsCount": 2,
  "durationMs": 4521,
  "timestamp": "2026-05-02T18:15:00Z"
}
```

### Ask a Question

```bash
# Via GET (add X-API-KEY header)
curl "http://localhost:8080/api/v1/chat/ask?message=How%20to%20build%20RAG%20with%20Spring%20AI?" \
  -H "X-API-KEY: nexus-admin-key-2024"

# Via POST with conversational memory
curl -X POST http://localhost:8080/api/v1/chat/ask \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: nexus-admin-key-2024" \
  -d '{"message": "How to build RAG with Spring AI?", "chatId": "session-1"}'
```

Expected response:
```json
{
  "question": "How to build RAG with Spring AI?",
  "answer": "To build a RAG application with Spring AI, you need...",
  "model": "gemini-2.5-flash",
  "timestamp": "2026-05-02T18:16:00Z"
}
```

---

## 7. API Reference

### Base URL

```
http://localhost:8080
```

### Interactive Documentation

| Tool | URL |
|---|---|
| **Swagger UI** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |
| **OpenAPI JSON** | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) |

### Endpoints

#### RAG Chat

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/chat/ask?message=...&chatId=...` | Ask a question (query param — browser-friendly) |
| `POST` | `/api/v1/chat/ask` | Ask a question (JSON body — production) |
| `GET` | `/api/v1/chat/faq?message=...` | Legacy compatibility endpoint |

**POST `/api/v1/chat/ask`** — Request Body:
```json
{
  "message": "How to build RAG with Spring AI and MongoDB?",
  "chatId": "user-session-123"
}
```

**Response** (`200 OK`):
```json
{
  "question": "How to build RAG with Spring AI and MongoDB?",
  "answer": "To build a RAG application...",
  "model": "gemini-2.5-flash",
  "timestamp": "2026-05-02T18:16:00.000Z"
}
```

#### Document Ingestion

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/docs/upload` | Upload and ingest a dynamic PDF document |
| `POST` | `/api/v1/docs/load` | Ingest static JSON documents (recommended) |
| `GET` | `/api/v1/docs/load` | Ingest static JSON documents (browser-friendly) |
| `DELETE` | `/api/v1/docs/clear?source=...` | Purge document chunks by source name |

**Response** (`200 OK`):
```json
{
  "status": "SUCCESS",
  "documentsCount": 2,
  "durationMs": 4521,
  "timestamp": "2026-05-02T18:15:00.000Z"
}
```

#### Health & Monitoring

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/actuator/health` | Public | Application health status |
| `GET` | `/actuator/info` | Public | Application metadata |
| `GET` | `/actuator/metrics` | Public | Application metrics |
| `GET` | `/actuator/prometheus` | Public | Prometheus scrape endpoint |

#### Error Responses

All errors follow a consistent schema:
```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "One or more fields failed validation",
  "details": ["message: must not be blank"],
  "path": "/api/v1/chat/ask",
  "timestamp": "2026-05-02T18:16:00.000Z"
}
```

---

## 8. Configuration Reference

All configuration lives in `src/main/resources/application.yml`:

| Property | Default | Description |
|---|---|---|
| `spring.ai.google.genai.api-key` | `${GEMINI_API_KEY}` | Google Gemini API key (env var) |
| `spring.ai.google.genai.chat.options.model` | `gemini-2.5-flash` | Gemini chat model |
| `spring.ai.google.genai.chat.options.temperature` | `0.7` | Response creativity (0.0–2.0) |
| `spring.ai.vectorstore.mongodb.initialize-schema` | `true` | Auto-create vector search index |
| `spring.ai.vectorstore.mongodb.collection-name` | `vector_store` | MongoDB collection for embeddings |
| `spring.ai.vectorstore.mongodb.index-name` | `vector_index` | Vector search index name |
| `spring.data.mongodb.uri` | `${MONGODB_URI}` | MongoDB connection string (env var) |
| `spring.data.mongodb.database` | `rag` | Database name |
| `server.port` | `${SERVER_PORT:8080}` | HTTP port |
| `app.security.api-key` | `nexus-admin-key-2024` | API key for X-API-KEY header auth |
| `resilience4j.ratelimiter.instances.chatLimit.limitForPeriod` | `10` | Max requests per minute |

---

## 9. Core Components Deep-Dive

### A. `RagController` — The AI Query Interface

**Path**: `/api/v1/chat`

The controller uses Spring AI's `ChatClient` builder pattern with a `MessageChatMemoryAdvisor` (for conversational history) and a `QuestionAnswerAdvisor` (for RAG). When a user sends a question:

1. The **memory advisor loads** the conversation history for the provided `chatId`.
2. The **RAG advisor intercepts** the user message before it reaches Gemini.
3. It **embeds the query** using Gemini's `text-embedding-004` model.
4. It **performs a vector similarity search** in MongoDB Atlas to find the top-5 most relevant document chunks.
5. It **augments the system prompt** by appending the retrieved documents as context.
6. It **forwards the full prompt** (history + user question + context) to Gemini 2.5 Flash.
7. The generated answer is returned wrapped in an `AskResponse` DTO.
8. The **memory advisor saves** the new exchange for the `chatId`.

The class is annotated with `@RateLimiter(name = "chatLimit")` (Resilience4j) to enforce the 10 req/min limit.

This is declarative RAG — zero manual embedding or search logic needed.

### B. `DocsLoaderService` — The ETL Pipeline

**Ingestion flow**:

1. **Read**: Loads `devcenter-content-snapshot.2024-05-20.json` from the classpath (JSONL format — one JSON object per line).
2. **Extract**: Parses each line into a Map, extracts the `body` field as content.
3. **Enrich**: Copies top-level fields (`title`, `sourceName`, `url`, `action`, `format`, `updated`) into the document metadata. This metadata is stored alongside the vector in MongoDB and can be used for filtering.
4. **Chunk**: Uses `TokenTextSplitter` (backed by a tokenizer like JTokkit) to split content into token-bounded chunks. This prevents LLM context window overflow and preserves semantic boundaries.
5. **Batch Store**: Sends documents to the `VectorStore` in batches of 100, which triggers:
   - Embedding generation via Gemini
   - Storage in MongoDB Atlas with the vector, content, and metadata

### C. `GlobalExceptionHandler` — Error Management

A `@RestControllerAdvice` that converts all exceptions into consistent `ApiError` JSON responses:

| Exception | HTTP Status | Error Key |
|---|---|---|
| `MethodArgumentNotValidException` | 400 | Validation Failed |
| `MissingServletRequestParameterException` | 400 | Missing Parameter |
| `IllegalArgumentException` | 400 | Bad Request |
| `IngestionException` | 500 | Ingestion Error |
| `Exception` (catch-all) | 500 | Internal Server Error |

### D. Configuration Classes

- **`AiConfig`**: Defines the `ChatMemory` bean using `MessageWindowChatMemory` backed by `InMemoryChatMemoryRepository`. Maintains a 20-message sliding window per `chatId`.
- **`SecurityConfig`**: Registers a custom `ApiKeyFilter` that checks the `X-API-KEY` request header. Permits Swagger and Actuator endpoints without a key. Sets a `UsernamePasswordAuthenticationToken` in the `SecurityContext` for valid keys.
- **`OpenApiConfig`**: Customizes Swagger UI with project metadata, API description, and adds the API key `SecurityScheme` so the Swagger UI shows an **Authorize** button.
- **`WebConfig`**: Centralized CORS policy — allows all origins (development-friendly, restrict in production).

---

## 10. Docker Deployment

### Multi-Stage Build Strategy

The `Dockerfile` uses a two-stage build:

| Stage | Base Image | Purpose |
|---|---|---|
| **Build** | `maven:3.9-eclipse-temurin-21` | Compile source, resolve dependencies, package fat JAR |
| **Run** | `eclipse-temurin:21-jre` | Run the compiled JAR (minimal image, ~200MB) |

**Security features**:
- Runs as a non-root `appuser` (principle of least privilege)
- JRE-only runtime image (no compiler, no build tools)
- Health check via Actuator

### Docker Compose Usage

```bash
# 1. Ensure .env file exists with your credentials
cp .env.example .env

# 2. Build and start
docker compose up --build

# 3. Stop
docker compose down
```

---

## 11. Monitoring & Health Checks

Spring Boot Actuator provides production-ready monitoring:

```bash
# Health check
curl http://localhost:8080/actuator/health
# → {"status":"UP"}

# Application info
curl http://localhost:8080/actuator/info
# → {"app":{"name":"Spring AI RAG","version":"1.0.0",...}}

# Metrics catalog
curl http://localhost:8080/actuator/metrics
```

---

## 12. Troubleshooting

### Common Issues

| Problem | Cause | Solution |
|---|---|---|
| `401` / `403` from Gemini | Invalid or expired API key | Regenerate key at [aistudio.google.com/apikey](https://aistudio.google.com/apikey) |
| `429 Too Many Requests` | Gemini free tier rate limit exceeded | Wait and retry, or consider a paid plan |
| `MongoTimeoutException` | MongoDB Atlas unreachable | Check Network Access whitelist, verify URI format |
| `Connection refused localhost:27017` | No local MongoDB and no Atlas URI set | Set `MONGODB_URI` to your Atlas connection string |
| `No qualifying bean of type VectorStore` | Missing MongoDB connection | Ensure `MONGODB_URI` is set correctly |
| `TokenTextSplitter` errors | Empty or malformed source JSON | Check `docs/devcenter-content-snapshot.2024-05-20.json` |
| Port 8080 already in use | Another process using the port | Set `SERVER_PORT=8081` in your environment |

### Checking Logs

```bash
# Maven
./mvnw spring-boot:run 2>&1 | grep -i error

# Docker
docker compose logs -f app
```

---

## 13. Future Extensibility

| Enhancement | Description |
|---|---|
| **Persistent Memory** | Swap `InMemoryChatMemoryRepository` for `JdbcChatMemoryRepository` or a MongoDB-backed store so history survives restarts |
| **Streaming Responses** | Return `Flux<String>` for real-time token streaming |
| **Hybrid Search** | Combine MongoDB Vector Search with Full-Text Search (BM25) for improved recall |
| **Re-ranking** | Add a cross-encoder model to re-rank retrieved documents for higher precision |
| **Async Ingestion** | Use `@Async` or Spring Batch for large dataset ingestion without blocking |
| **Frontend UI** | Vite + React chat interface with streaming support and document management dashboard |

---

## Dependency Inventory

| Dependency | GroupId | ArtifactId | Version | Purpose |
|---|---|---|---|---|
| Spring Boot Web | `org.springframework.boot` | `spring-boot-starter-web` | 3.5.5 | Embedded Tomcat, Spring MVC REST |
| Spring Boot Actuator | `org.springframework.boot` | `spring-boot-starter-actuator` | 3.5.5 | Health, info, metrics, Prometheus endpoints |
| Spring Boot Validation | `org.springframework.boot` | `spring-boot-starter-validation` | 3.5.5 | Bean validation (Jakarta) |
| Spring Boot Security | `org.springframework.boot` | `spring-boot-starter-security` | 3.5.5 | API key filter chain |
| Spring AI Google GenAI | `org.springframework.ai` | `spring-ai-starter-model-google-genai` | 1.1.5 | Google Gemini 2.5 Flash chat + embedding |
| Spring AI MongoDB | `org.springframework.ai` | `spring-ai-starter-vector-store-mongodb-atlas` | 1.1.5 | MongoDB Atlas vector store auto-config |
| Spring AI PDF Reader | `org.springframework.ai` | `spring-ai-pdf-document-reader` | 1.1.5 | Dynamic PDF ingestion |
| Micrometer Prometheus | `io.micrometer` | `micrometer-registry-prometheus` | (managed) | Prometheus metrics export |
| Resilience4j | `io.github.resilience4j` | `resilience4j-spring-boot3` | 2.2.0 | Rate limiting (10 req/min) |
| SpringDoc OpenAPI | `org.springdoc` | `springdoc-openapi-starter-webmvc-ui` | 2.8.5 | Swagger UI + OpenAPI 3.0 |
| spring-dotenv | `me.paulschwarz` | `spring-dotenv` | (latest) | Native `.env` file loading |
| Spring Boot Test | `org.springframework.boot` | `spring-boot-starter-test` | 3.5.5 | JUnit 5, Mockito, AssertJ |

---

*Built with Spring AI, MongoDB Atlas, and Google Gemini.*
