package com.nexus.rag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.rag.exception.IngestionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service responsible for the document ingestion ETL pipeline.
 *
 * <p>Reads a JSONL (JSON-Lines) file from the classpath, extracts content and metadata,
 * chunks documents using a token-aware splitter, and stores the resulting embeddings
 * into the MongoDB Atlas Vector Store.</p>
 *
 * <h3>Pipeline Flow:</h3>
 * <ol>
 *   <li>Read each JSON line from the source file</li>
 *   <li>Extract the body content and enrichment metadata</li>
 *   <li>Split content into token-bounded chunks via {@link TokenTextSplitter}</li>
 *   <li>Batch-ingest chunks into the {@link VectorStore} (100 per batch)</li>
 * </ol>
 */
@Service
public class DocsLoaderService {

    private static final Logger logger = LoggerFactory.getLogger(DocsLoaderService.class);
    private static final int BATCH_SIZE = 100;
    private static final String DATA_SOURCE = "docs/devcenter-content-snapshot.2024-05-20.json";

    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;
    private final TokenTextSplitter tokenTextSplitter;

    public DocsLoaderService(VectorStore vectorStore, ObjectMapper objectMapper) {
        this.vectorStore = vectorStore;
        this.objectMapper = objectMapper;
        // Default configuration: ~800 tokens chunk size, ~350 tokens overlap
        this.tokenTextSplitter = new TokenTextSplitter();
    }

    /**
     * Executes the full document ingestion pipeline.
     *
     * @return the total number of document chunks ingested into the vector store.
     * @throws IngestionException if the ingestion process fails.
     */
    public int loadDocs() {
        var resource = new ClassPathResource(DATA_SOURCE);
        AtomicInteger totalChunks = new AtomicInteger(0);

        try (var reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            List<Document> buffer = new ArrayList<>();
            String line;
            int lineCount = 0;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                lineCount++;

                @SuppressWarnings("unchecked")
                Map<String, Object> jsonDoc = objectMapper.readValue(line, Map.class);
                String content = (String) jsonDoc.get("body");

                if (content == null || content.isBlank()) {
                    logger.warn("Skipping line {} — empty or missing 'body' field", lineCount);
                    continue;
                }

                // Create base document with enriched metadata
                var metadata = extractMetadata(jsonDoc);
                var baseDocument = new Document(content, metadata);

                // Split into token-bounded chunks
                var chunks = tokenTextSplitter.split(baseDocument);
                buffer.addAll(chunks);

                // Batch flush
                if (buffer.size() >= BATCH_SIZE) {
                    ingestBatch(buffer);
                    totalChunks.addAndGet(buffer.size());
                    buffer.clear();
                }
            }

            // Flush remaining documents
            if (!buffer.isEmpty()) {
                totalChunks.addAndGet(buffer.size());
                ingestBatch(buffer);
                buffer.clear();
            }

            logger.info("Ingestion complete. Total chunks ingested: {}", totalChunks.get());
            return totalChunks.get();

        } catch (Exception e) {
            logger.error("Document ingestion pipeline failed", e);
            throw new IngestionException("Failed to ingest documents: " + e.getMessage(), e);
        }
    }

    /**
     * Sends a batch of documents to the vector store.
     */
    private void ingestBatch(List<Document> documents) {
        logger.info("Ingesting batch of {} document chunks", documents.size());
        vectorStore.add(new ArrayList<>(documents));
    }

    /**
     * Extracts and enriches metadata from a parsed JSON document.
     *
     * <p>Copies top-level fields (title, source, url, etc.) into the metadata map
     * so they are persisted alongside the vector embedding in MongoDB.</p>
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMetadata(Map<String, Object> jsonMap) {
        Map<String, Object> metadata = new java.util.HashMap<>(
                (Map<String, Object>) jsonMap.getOrDefault("metadata", Map.of())
        );

        // Enrich with top-level fields
        List.of("title", "sourceName", "url", "action", "format", "updated")
                .forEach(field -> {
                    if (jsonMap.containsKey(field)) {
                        metadata.put(field, jsonMap.get(field));
                    }
                });

        return metadata;
    }

    /**
     * Purges all document chunks from the vector store that match a specific source name.
     *
     * @param sourceName The source name to match in metadata (e.g. filename)
     */
    public void deleteBySource(String sourceName) {
        logger.info("Purging document chunks for source: {}", sourceName);
        // Using Spring AI string-based filter expression for simplicity
        vectorStore.delete("sourceName == '" + sourceName + "'");
    }

    /**
     * Reads an uploaded PDF file, extracts text, chunks it, and saves it to the vector store.
     *
     * @param file The uploaded MultipartFile (PDF)
     * @return Number of chunks ingested
     */
    public int loadPdf(MultipartFile file) throws Exception {
        byte[] bytes = file.getBytes();
        Resource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource,
                PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(org.springframework.ai.reader.ExtractedTextFormatter.builder().build())
                        .build());

        List<Document> documents = pdfReader.get();
        List<Document> chunks = tokenTextSplitter.split(documents);

        if (!chunks.isEmpty()) {
            vectorStore.add(chunks);
            logger.info("Successfully ingested PDF: {} in {} chunks", file.getOriginalFilename(), chunks.size());
        }

        return chunks.size();
    }
}
