package com.studentregistration.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * GlobalExceptionHandler — the single place that turns exceptions into HTTP error
 * responses. {@code @RestControllerAdvice} makes these handlers apply to EVERY
 * controller.
 *
 * <p><b>Why this is critical:</b> the React frontend calls {@code response.json()}
 * unconditionally on the mutating endpoints and reads a top-level {@code error}
 * string. Spring's DEFAULT error JSON uses different keys
 * ({@code timestamp/status/error/message/path}) where {@code error} is a reason
 * phrase like {@code "Bad Request"} — not our message. So we must produce exactly
 * {@code {"error": "<our message>"}} for every failure, which is what
 * {@link ErrorResponse} below serializes to.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** The one and only error body shape: {@code {"error": "..."}}. */
    public record ErrorResponse(String error) {
    }

    /**
     * Our own business/validation errors carry the exact status + message the Flask
     * routes returned (400 "…required.", 404 "Student not found.", etc.).
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ErrorResponse(ex.getMessage()));
    }

    /**
     * The whole request body exceeded the multipart limit (55 MB). This is the
     * Java equivalent of Flask's 413 handler for {@code MAX_CONTENT_LENGTH}, and we
     * reproduce its exact message.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ErrorResponse("Upload too large. Each PDF must be 25 MB or less."));
    }

    /**
     * Safety net for anything unforeseen. Returning JSON (rather than Spring's default
     * error page) keeps the frontend's {@code response.json()} calls from choking. The
     * message is included to aid debugging in this study project.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Internal server error.";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(message));
    }
}
