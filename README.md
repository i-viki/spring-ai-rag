# Spring AI RAG

<p align="center">
  <img src="https://img.shields.io/badge/Spring_Boot-3.5.5-6DB33F?style=flat&logo=spring-boot&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/Spring_AI-1.1.5-6DB33F?style=flat&logo=spring&logoColor=white" alt="Spring AI"/>
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white" alt="Java 21"/>
  <img src="https://img.shields.io/badge/MongoDB_Atlas-47A248?style=flat&logo=mongodb&logoColor=white" alt="MongoDB Atlas"/>
  <img src="https://img.shields.io/badge/Google_Gemini-8E75B2?style=flat&logo=google-gemini&logoColor=white" alt="Google Gemini"/>
  <img src="https://img.shields.io/badge/Docker-Ready-2496ED?style=flat&logo=docker&logoColor=white" alt="Docker"/>
  <img src="https://img.shields.io/badge/License-Apache_2.0-blue?style=flat" alt="License"/>
</p>

> **Production-ready Retrieval-Augmented Generation (RAG) system** powered by Spring Boot, Spring AI, MongoDB Atlas Vector Search, and **Google Gemini** (free tier).

Ask questions and get AI-generated answers grounded in **your own documents** — not just the model's training data.

---

## Features

- **Declarative RAG** — Spring AI's `ChatClient` + `QuestionAnswerAdvisor` handles embedding, search, and prompt augmentation automatically
- **Smart Document Ingestion** — Token-aware chunking via `TokenTextSplitter` with dynamic PDF and JSON file upload support
- **Vector Search** — MongoDB Atlas Vector Search for high-performance similarity queries
- **Free AI Tier** — Uses Gemini 2.5 Flash / text-embedding-004 — **no paid API key required**
- **Enterprise Security** — API protection via `X-API-KEY` header with Spring Security
- **Rate Limiting** — Resilience4j limits abuse to 10 requests/minute
- **Conversational Memory** — `chatId`-based session memory with a 20-message sliding window
- **Observability** — Spring Boot Actuator + Micrometer + Prometheus metrics endpoint
- **Swagger UI** — Interactive OpenAPI 3.0 documentation with API key authorization support
- **Docker Ready** — Multi-stage Dockerfile + Docker Compose with health checks
- **Error Handling** — Global `@ControllerAdvice` with consistent JSON error responses
- **Validation** — Request body validation with Jakarta Bean Validation

---

## Architecture

```
User Question --> RagController --> MessageChatMemoryAdvisor --> QuestionAnswerAdvisor
                                          |                              |
                                   Load session history          Embed + Search
                                   (InMemory, 20 msgs)         (MongoDB Atlas)
                                                                         |
                                                                  Augment Prompt
                                                                         |
                                                              Google Gemini 2.5 Flash
                                                                         |
                                                               AI-Powered Answer
```

---

## Prerequisites

| Software | Version | Required |
|---|---|---|
| **Java JDK** | 21 (LTS) | Yes |
| **Maven** | 3.9+ | Yes (included via `mvnw` wrapper) |
| **Gemini API Key** | — | Yes — [Get one FREE](https://aistudio.google.com/apikey) |
| **MongoDB Atlas** | M0+ | Yes — [Free tier](https://cloud.mongodb.com) |
| **Docker** | 24+ | Optional |

---

## Quick Start

### 1. Clone & Configure

```bash
git clone https://github.com/i-viki/spring-ai-rag.git
cd spring-ai-rag

# Copy and fill in your credentials
cp .env.example .env
```

Edit `.env`:
```properties
GEMINI_API_KEY=your-gemini-api-key-here
MONGODB_URI=mongodb+srv://user:pass@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority
```

### 2. Run

```bash
# Linux / macOS — the .env file is loaded automatically via spring-dotenv
./mvnw spring-boot:run

# Windows
.\mvnw.cmd spring-boot:run
```

### 3. API Security

All endpoints (except Actuator and Swagger) require the following header:

- **Header**: `X-API-KEY`
- **Default Key**: `nexus-admin-key-2024`
- **Change it**: set `app.security.api-key` in `application.yml`

> **Windows users**: Use `curl.exe` instead of `curl` to avoid PowerShell aliasing issues.
> For JSON bodies, save your payload to a file and pass it with `-d @request.json`.

---

### 4. Ingest Knowledge Base

Load the bundled sample documents into MongoDB Atlas:

```bash
curl -X POST http://localhost:8080/api/v1/docs/load \
  -H "X-API-KEY: nexus-admin-key-2024"
```

### 5. Ask a Question

Provide a `chatId` to maintain conversational context across requests:

```bash
curl -X POST http://localhost:8080/api/v1/chat/ask \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: nexus-admin-key-2024" \
  -d '{
    "message": "What is Spring AI?",
    "chatId": "session-123"
  }'
```

**Response:**
```json
{
  "question": "What is Spring AI?",
  "answer": "Spring AI is a framework that ...",
  "model": "gemini-2.5-flash",
  "timestamp": "2026-05-02T18:16:00Z"
}
```

### 6. Upload a PDF

```bash
curl -X POST http://localhost:8080/api/v1/docs/upload \
  -H "X-API-KEY: nexus-admin-key-2024" \
  -F "file=@/path/to/document.pdf"
```

### 7. Clear Documents by Source

```bash
curl -X DELETE "http://localhost:8080/api/v1/docs/clear?source=document.pdf" \
  -H "X-API-KEY: nexus-admin-key-2024"
```

---

## Docker

```bash
# Build and run (reads .env automatically)
docker compose up --build

# Stop
docker compose down
```

---

## API Reference

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/chat/ask?message=...&chatId=...` | Required | Ask a question (browser-friendly) |
| `POST` | `/api/v1/chat/ask` | Required | Ask a question with conversational memory |
| `GET` | `/api/v1/chat/faq?message=...` | Required | Legacy compatibility endpoint |
| `POST` | `/api/v1/docs/load` | Required | Ingest static JSON documents into vector store |
| `GET` | `/api/v1/docs/load` | Required | Ingest static JSON documents (browser-friendly) |
| `POST` | `/api/v1/docs/upload` | Required | Upload and ingest a PDF document |
| `DELETE` | `/api/v1/docs/clear?source=...` | Required | Purge all chunks matching a source name |
| `GET` | `/actuator/health` | Public | Application health status |
| `GET` | `/actuator/metrics` | Public | Application metrics |
| `GET` | `/actuator/prometheus` | Public | Prometheus scrape endpoint |
| `GET` | `/swagger-ui.html` | Public | Interactive API documentation |

> **Full API docs**: `http://localhost:8080/swagger-ui.html` (after starting the app)

---

## Project Structure

```
src/main/java/com/nexus/rag/
├── SpringAiRagApplication.java      # Entry point
├── config/
│   ├── AiConfig.java                # ChatMemory bean (conversational history)
│   ├── OpenApiConfig.java           # Swagger UI + API key security scheme
│   ├── SecurityConfig.java          # X-API-KEY filter + Spring Security
│   └── WebConfig.java               # CORS policy (all origins allowed)
├── controller/
│   ├── DocsLoaderController.java    # /api/v1/docs -- ingestion, upload, clear
│   └── RagController.java           # /api/v1/chat -- ask, faq (rate-limited)
├── dto/
│   ├── ApiError.java                # Consistent error response schema
│   ├── AskRequest.java              # Chat request (message + optional chatId)
│   ├── AskResponse.java             # Chat response (question, answer, model, timestamp)
│   └── IngestionResponse.java       # Ingestion result (status, count, duration)
├── exception/
│   ├── GlobalExceptionHandler.java  # Global @ControllerAdvice
│   └── IngestionException.java      # Custom ingestion failure exception
└── service/
    └── DocsLoaderService.java       # ETL pipeline (JSON + PDF ingestion, delete)
```

---

## Documentation

For the complete technical deep-dive including architecture diagrams, configuration reference, troubleshooting guide, and deployment instructions, see:

**[docs/detailed_doc.md](docs/detailed_doc.md)**

---

## Tech Stack

| Component | Technology |
|---|---|
| **Runtime** | Java 21 (Eclipse Temurin) |
| **Framework** | Spring Boot 3.5.5 |
| **AI Orchestration** | Spring AI 1.1.5 |
| **LLM / Embedding** | Google Gemini 2.5 Flash + text-embedding-004 (Free Tier) |
| **Vector Database** | MongoDB Atlas Vector Search |
| **Conversational Memory** | Spring AI `MessageWindowChatMemory` (in-memory, 20-message window) |
| **Security** | Spring Security + custom `X-API-KEY` header filter |
| **Rate Limiting** | Resilience4j (10 req/min per instance) |
| **PDF Ingestion** | Spring AI PDF Document Reader + `TokenTextSplitter` |
| **Observability** | Spring Boot Actuator + Micrometer + Prometheus |
| **API Docs** | SpringDoc OpenAPI 2.8.5 (Swagger UI with API key authorization) |
| **Environment** | `spring-dotenv` for native `.env` file loading |
| **Build** | Maven 3.9+ (wrapper included) |
| **Container** | Docker + Docker Compose |

---

## License

This project is licensed under the [Apache License 2.0](LICENSE).

---

## Author

**Jayavignesh J**

[![Website](https://img.shields.io/badge/Website-jayavignesh.dev-0A66C2?style=flat&logo=google-chrome&logoColor=white)](https://jayavignesh.dev)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-jayavigneshj-0A66C2?style=flat&logo=linkedin&logoColor=white)](https://linkedin.com/in/jayavigneshj)
[![GitHub](https://img.shields.io/badge/GitHub-i--viki-181717?style=flat&logo=github&logoColor=white)](https://github.com/i-viki)

---

<p align="center">Built with Spring AI, MongoDB Atlas, and Google Gemini</p>
