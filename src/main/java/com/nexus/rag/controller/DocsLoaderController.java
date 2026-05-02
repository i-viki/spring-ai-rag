package com.nexus.rag.controller;

import com.nexus.rag.dto.IngestionResponse;
import com.nexus.rag.service.DocsLoaderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for document ingestion operations.
 *
 * <p>Triggers the ETL pipeline that reads source documents, chunks them,
 * generates vector embeddings, and stores them in MongoDB Atlas.</p>
 */
@RestController
@RequestMapping("/api/v1/docs")
@Tag(name = "Document Ingestion", description = "Endpoints for loading and preparing documents for the RAG pipeline.")
public class DocsLoaderController {

    private final DocsLoaderService docsLoaderService;

    public DocsLoaderController(DocsLoaderService docsLoaderService) {
        this.docsLoaderService = docsLoaderService;
    }

    /**
     * Triggers the document ingestion pipeline (POST — recommended for production).
     */
    @PostMapping("/load")
    @Operation(
            summary = "Ingest documents",
            description = "Reads the source JSON data, chunks it using TokenTextSplitter, generates embeddings via OpenAI, and stores them in MongoDB Atlas Vector Store.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ingestion completed successfully",
                            content = @Content(schema = @Schema(implementation = IngestionResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Ingestion failed")
            }
    )
    public ResponseEntity<IngestionResponse> loadDocuments() {
        long startTime = System.currentTimeMillis();
        int count = docsLoaderService.loadDocs();
        long durationMs = System.currentTimeMillis() - startTime;
        return ResponseEntity.ok(IngestionResponse.success(count, durationMs));
    }

    /**
     * Triggers the document ingestion pipeline (GET — for easy browser/curl testing).
     */
    @GetMapping("/load")
    @Operation(
            summary = "Ingest documents (GET)",
            description = "Convenience GET endpoint for triggering ingestion from a browser. Prefer POST for production use."
    )
    public ResponseEntity<IngestionResponse> loadDocumentsGet() {
        return loadDocuments();
    }

    /**
     * Uploads a PDF file dynamically and ingests it into MongoDB.
     */
    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload and ingest a PDF document",
            description = "Upload a PDF file. The application will extract the text, split it into chunks, generate embeddings, and store them in MongoDB."
    )
    public ResponseEntity<IngestionResponse> uploadPdf(@org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        long startTime = System.currentTimeMillis();
        try {
            int count = docsLoaderService.loadPdf(file);
            long durationMs = System.currentTimeMillis() - startTime;
            return ResponseEntity.ok(IngestionResponse.success(count, durationMs));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(IngestionResponse.failure(e.getMessage()));
        }
    }

    /**
     * Deletes all documents matching a specific source name.
     */
    @DeleteMapping("/clear")
    @Operation(
            summary = "Clear documents by source",
            description = "Removes all ingested document chunks from the vector store that match the provided source name (e.g. filename)."
    )
    public ResponseEntity<String> deleteDocuments(@RequestParam("source") String source) {
        docsLoaderService.deleteBySource(source);
        return ResponseEntity.ok("Successfully purged documents with source: " + source);
    }
}
