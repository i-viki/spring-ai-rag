package com.nexus.rag.exception;

import com.nexus.rag.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Global exception handler for the entire application.
 *
 * <p>Converts all unhandled exceptions into a consistent {@link ApiError} JSON body
 * with the appropriate HTTP status code.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles bean validation errors (e.g. {@code @Valid} on request DTOs).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList();

        var error = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "One or more fields failed validation",
                details,
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Handles missing required request parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex,
                                                       HttpServletRequest request) {
        var error = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Missing Parameter",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Handles document ingestion failures.
     */
    @ExceptionHandler(IngestionException.class)
    public ResponseEntity<ApiError> handleIngestion(IngestionException ex,
                                                    HttpServletRequest request) {
        logger.error("Document ingestion failed", ex);
        var error = ApiError.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Ingestion Error",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.internalServerError().body(error);
    }

    /**
     * Handles illegal argument exceptions (e.g. bad input data).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex,
                                                          HttpServletRequest request) {
        var error = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Catch-all handler for any unhandled exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception ex, HttpServletRequest request) {
        logger.error("Unhandled exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        var error = ApiError.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        );
        return ResponseEntity.internalServerError().body(error);
    }
}
