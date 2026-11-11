# Spring AI RAG Engine

[![Java](https://img.shields.io/badge/Java-21%20LTS-18181b?logo=openjdk&logoColor=ED8B00&labelColor=101012)](https://adoptium.net)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-18181b?logo=springboot&logoColor=6DB33F&labelColor=101012)](https://spring.io)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.1.5-18181b?logo=spring&logoColor=6DB33F&labelColor=101012)](https://spring.io/projects/spring-ai)
[![MongoDB Atlas](https://img.shields.io/badge/MongoDB_Atlas-Vector_Search-18181b?logo=mongodb&logoColor=47A248&labelColor=101012)](https://cloud.mongodb.com)
[![Gemini](https://img.shields.io/badge/Gemini_Flash-2.5-18181b?logo=google-gemini&logoColor=4285F4&labelColor=101012)](https://deepmind.google/technologies/gemini/)
[![Resilience4j](https://img.shields.io/badge/Resilience4j-Rate_Limiter-18181b?logo=resilience4j&logoColor=F58220&labelColor=101012)](https://resilience4j.readme.io)
[![Swagger](https://img.shields.io/badge/Swagger-OpenAPI_3-18181b?logo=swagger&logoColor=85EA2D&labelColor=101012)](https://swagger.io)
[![Docker](https://img.shields.io/badge/Docker-Ready-18181b?logo=docker&logoColor=2496ED&labelColor=101012)](https://www.docker.com)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-yellow.svg)](LICENSE)

A production-ready **enterprise Retrieval-Augmented Generation (RAG) platform** engineered with **Java 21 LTS**, **Spring Boot 3.5.5**, **Spring AI 1.1.5**, and **MongoDB Atlas Vector Search**. Powered by **Google Gemini 2.5 Flash** and **text-embedding-004**, the engine automates ETL document chunking (`TokenTextSplitter`), high-dimensional cosine similarity indexing, sliding-window conversational memory (`MessageWindowChatMemory`), and resilient API governance using **Resilience4j** rate limiting.

---

## Key Architecture Pillars

- **Declarative RAG & Advisor Pipeline (`com.nexus.rag.controller.RagController`):**
  Seamless orchestration pairing Spring AI's `ChatClient` with `QuestionAnswerAdvisor` to execute embedding lookups, vector matching, and grounded context augmentation transparently.
- **Token-Aware ETL Ingestion Engine (`com.nexus.rag.service.DocsLoaderService`):**
  High-throughput ingestion pipeline splitting PDFs (Apache PDFBox) and structured JSON documents into semantic tokens via `TokenTextSplitter` before writing 768-dimensional embeddings to MongoDB Atlas.
- **Conversational Memory Window (`com.nexus.rag.config.AiConfig`):**
  In-memory sliding session buffer (`MessageWindowChatMemory`) preserving the last 20 conversational turns per `chatId` to support coherent multi-turn reasoning.
- **Enterprise API Key Security & Rate Limiting (`com.nexus.rag.config.SecurityConfig`):**
  Zero-trust API protection requiring `X-API-KEY` verification paired with Resilience4j leaky bucket throttling (10 requests/minute per client instance).
- **End-to-End Metric Telemetry (`com.nexus.rag`):**
  Comprehensive observability via Spring Boot Actuator, Micrometer, and Prometheus scraping endpoints (`/actuator/prometheus`) tracking ingestion latency, token consumption, and JVM resource limits.

---

## System Architecture

```mermaid
flowchart TB
    subgraph CLIENT["Consumer & Integration Tier"]
        CLIENT_APP["API Consumer / Frontend<br/>Port: Client Dependent"]
        SWAGGER["Swagger UI Studio<br/>Port: 8080/swagger-ui.html"]
    end

    subgraph GATEWAY["Security & Ingress Gateway (Java 21 / Spring Boot)"]
        direction TB
        SEC_FILTER["ApiKeyAuthFilter<br/>• X-API-KEY Validation"]
        RATE_LIMIT["Resilience4j Rate Limiter<br/>• 10 req / min Bucket"]
        SEC_FILTER --> RATE_LIMIT
    end

    subgraph CONTROLLERS["API Endpoints Tier"]
        RAG_CTRL["RagController<br/>• POST /api/v1/chat/ask<br/>• GET /api/v1/chat/faq"]
        DOCS_CTRL["DocsLoaderController<br/>• POST /api/v1/docs/upload<br/>• POST /api/v1/docs/load<br/>• DELETE /api/v1/docs/clear"]
    end

    subgraph RAG_PIPELINE["Spring AI Declarative RAG Engine"]
        ADVISOR["QuestionAnswerAdvisor<br/>• Prompt Augmentation<br/>• Similarity Thresholding"]
        MEMORY["MessageWindowChatMemory<br/>• 20-Turn Sliding Window<br/>• Session Isolation by chatId"]
        CHAT_CLIENT["Spring AI ChatClient<br/>• Model: Gemini 2.5 Flash"]
        ADVISOR <--> MEMORY
        ADVISOR --> CHAT_CLIENT
    end

    subgraph ETL_ENGINE["Document ETL Pipeline"]
        SPLITTER["TokenTextSplitter<br/>• 800-Token Chunking<br/>• 100-Token Overlap"]
        PDF_READER["Apache PDFBox Ingestor<br/>• Dynamic PDF Extraction"]
        SPLITTER --> PDF_READER
    end

    subgraph STORAGE["Vector & Telemetry Stores"]
        ATLAS[("MongoDB Atlas Vector Search<br/>• 768-dim Embeddings<br/>• Cosine Similarity Index")]
        PROM["Prometheus Metrics Hub<br/>• /actuator/prometheus"]
    end

    CLIENT -->|HTTP + X-API-KEY| GATEWAY
    SWAGGER -->|Interactive Auth| GATEWAY
    GATEWAY --> CONTROLLERS
    
    RAG_CTRL --> RAG_PIPELINE
    DOCS_CTRL --> ETL_ENGINE
    
    ETL_ENGINE -->|text-embedding-004| ATLAS
    RAG_PIPELINE <-->|Similarity Search| ATLAS
    GATEWAY -.->|Scrape Spans| PROM
```

### Retrieval & Augmentation Workflow

```text
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

### Project Structure

```text
src/main/java/com/nexus/rag/
├── SpringAiRagApplication.java      # Application bootstrap
├── config/
│   ├── AiConfig.java                # ChatMemory bean (conversational history)
│   ├── OpenApiConfig.java           # Swagger UI + API key security scheme
│   ├── SecurityConfig.java          # X-API-KEY filter + Spring Security
│   └── WebConfig.java               # CORS policy configuration
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

> For deep architectural explanations, see the comprehensive [docs/detailed_doc.md](docs/detailed_doc.md).

---

## Vector Cosine Similarity & Dynamic Chunking Invariants

Spring AI RAG eliminates semantic blindspots through mathematical vector similarity scoring and bounded token windowing:

$$\operatorname{CosineSimilarity}(\mathbf{u}, \mathbf{v}) = \frac{\mathbf{u} \cdot \mathbf{v}}{\|\mathbf{u}\|_2 \|\mathbf{v}\|_2} = \frac{\sum_{i=1}^{n} u_i v_i}{\sqrt{\sum_{i=1}^n u_i^2} \sqrt{\sum_{i=1}^n v_i^2}}$$

| Configuration Parameter | Bound / Specification | Operational Justification |
| :--- | :---: | :--- |
| **Embedding Dimension** | `768 dimensions` | Native alignment with Google `text-embedding-004` vector space |
| **Chunk Window Size** | `800 tokens` | Preserves semantic paragraph cohesion without exceeding LLM context |
| **Chunk Overlap** | `100 tokens` | Eliminates boundary truncation across adjacent sentence splits |
| **Rate Limit Threshold** | `10 requests / min` | Protects free-tier Gemini API quotas from exhaustion |
| **Session Window** | `20 messages` | Retains immediate conversational state while bounding memory footprint |

---

## Core API Specification

| Method & Path | Authorization | Purpose / Description |
| :--- | :---: | :--- |
| `POST /api/v1/chat/ask` | `X-API-KEY` | Primary endpoint for asking questions with session memory (`chatId`). |
| `GET /api/v1/chat/ask` | `X-API-KEY` | Query parameter interface (`?message=...&chatId=...`) for browser testing. |
| `GET /api/v1/chat/faq` | `X-API-KEY` | Legacy compatibility endpoint for quick FAQ retrieval. |
| `POST /api/v1/docs/load`| `X-API-KEY` | Ingests bundled sample JSON knowledge base into MongoDB Atlas. |
| `GET /api/v1/docs/load` | `X-API-KEY` | Browser-friendly GET trigger for document seeding. |
| `POST /api/v1/docs/upload`| `X-API-KEY`| Multipart endpoint to upload and vectorize PDF documents dynamically. |
| `DELETE /api/v1/docs/clear`| `X-API-KEY`| Purges all vectorized chunks matching a given `?source=...` reference. |
| `GET /actuator/health` | Public | Application health, database liveness, and readiness probes. |
| `GET /actuator/metrics`| Public | Micrometer runtime execution metrics. |
| `GET /actuator/prometheus`| Public | Prometheus time-series scrape endpoint. |
| `GET /swagger-ui.html` | Public | Interactive OpenAPI 3.0 documentation and execution sandbox. |

---

## Quickstart & Platform Orchestration

### Prerequisites
- Java 21 LTS & Maven 3.9+ (wrapper included)
- Google AI Studio API Key ([Get API Key](https://aistudio.google.com/apikey))
- MongoDB Atlas Cluster M0+ ([Free MongoDB Cloud Account](https://cloud.mongodb.com))
- Docker (Optional)

---

### Option A: Automated Docker Compose Orchestration (Recommended)

Launch the containerized RAG service in a single command:
```bash
# Clone repository
git clone https://github.com/i-viki/spring-ai-rag.git
cd spring-ai-rag

# Configure environment variables
cp .env.example .env
# Edit .env with your GEMINI_API_KEY and MONGODB_URI

# Launch container
docker compose up --build -d
```
*Application boots on `http://localhost:8080` with health checks verified.*

To cleanly tear down:
```bash
docker compose down
```

---

### Option B: Step-by-Step Manual Launch

#### Step 1: Configure Local Secrets
Populate `.env` in the project root:
```properties
GEMINI_API_KEY=your-gemini-api-key-here
MONGODB_URI=mongodb+srv://user:password@cluster0.xxxxx.mongodb.net/rag_db?retryWrites=true&w=majority
```

#### Step 2: Boot Application via Maven Wrapper
```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows PowerShell
.\mvnw.cmd spring-boot:run
```

#### Step 3: Seed Sample Knowledge Base
```bash
curl -X POST http://localhost:8080/api/v1/docs/load \
  -H "X-API-KEY: nexus-admin-key-2024"
```

---

## Decoupled Environment Strategy

All credentials and vector cluster endpoints are extracted into clean environment variables loaded natively via `spring-dotenv`:

| Service / Layer | Config File | Template Reference | Key Variables |
| :--- | :--- | :--- | :--- |
| **AI LLM & Embeddings** | `application.yml` | [`.env.example`](.env.example) | `GEMINI_API_KEY`, `SPRING_AI_GEMINI_CHAT_MODEL` |
| **Vector Store** | `application.yml` | [`.env.example`](.env.example) | `MONGODB_URI`, `SPRING_DATA_MONGODB_DATABASE` |
| **Security Perimeter** | `application.yml` | [`.env.example`](.env.example) | `APP_SECURITY_API_KEY` (Default: `nexus-admin-key-2024`) |
| **Rate Limiter** | `application.yml` | [`.env.example`](.env.example) | `RESILIENCE4J_RATELIMITER_INSTANCES_CHAT_LIMIT_FOR_PERIOD` |

---

## Live Telemetry & Observability Hubs

| Hub / Service | Port | URL | Role & Live Features |
| :--- | :--- | :--- | :--- |
| **Interactive Swagger UI** | `8080` | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | OpenAPI 3.0 interactive execution sandbox with `X-API-KEY` authorization. |
| **OpenAPI Specification** | `8080` | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) | Raw OpenAPI schema for SDK generation. |
| **Prometheus Metrics Hub** | `8080` | [http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus) | Time-series scraper publishing JVM heap, request latencies, and token counters. |
| **Spring Actuator Health** | `8080` | [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) | Live liveness and readiness probe status. |

---

## Testing & Quality Assurance Suite

### Application Unit & Integration Tests
```bash
./mvnw test
# Validates rate-limiter fallback logic, DTO serialization, and advisor injection.
```

### Conversational Chat Query Test
```bash
curl -X POST http://localhost:8080/api/v1/chat/ask \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: nexus-admin-key-2024" \
  -d '{
    "message": "What is Spring AI?",
    "chatId": "session-123"
  }'
```

**Sample Response:**
```json
{
  "question": "What is Spring AI?",
  "answer": "Spring AI is an application framework that provides a modular abstraction...",
  "model": "gemini-2.5-flash",
  "timestamp": "2026-05-02T18:16:00Z"
}
```

### PDF Ingestion Upload Test
```bash
curl -X POST http://localhost:8080/api/v1/docs/upload \
  -H "X-API-KEY: nexus-admin-key-2024" \
  -F "file=@/path/to/document.pdf"
```

### Purge Ingested Documents
```bash
curl -X DELETE "http://localhost:8080/api/v1/docs/clear?source=document.pdf" \
  -H "X-API-KEY: nexus-admin-key-2024"
```

---

## Author

**Jayavignesh**
- **Website:** [jayavignesh.dev](https://jayavignesh.dev)
- **GitHub:** [@i-viki](https://github.com/i-viki)
- **Email:** [hello@jayavignesh.dev](mailto:hello@jayavignesh.dev)

---

## License

This platform is distributed under the **Apache License 2.0**. See the [`LICENSE`](LICENSE) file for complete terms.
