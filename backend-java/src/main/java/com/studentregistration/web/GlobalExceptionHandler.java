package com.studentregistration.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    /**
     * SLF4J logger — the FIRST logger introduced in this codebase. It exists so that
     * unexpected 500s (see {@link #handleUnexpected}) are not invisible: the full
     * exception and stack trace are written to the SERVER log while the CLIENT only ever
     * sees a generic message. {@code static final} because a single, shared, immutable
     * logger per class is the standard SLF4J idiom (no reason to build one per request).
     */
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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
     * Client sent a body Spring/Jackson could not parse (malformed JSON, or a JSON
     * value of the wrong type for a field). Without this handler the exception would
     * fall through to the catch-all below and surface as a <b>500</b> whose body
     * echoes Jackson's internal parse message (e.g. class names / offsets) — both the
     * wrong status AND an internal-detail leak. We map it to a clean <b>400</b>.
     *
     * <p>Parity note: a genuinely empty body is fine — the controllers declare
     * {@code @RequestBody(required = false)}, matching Flask's {@code get_json() or {}},
     * so only a NON-empty malformed body reaches here.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Invalid or malformed request body."));
    }

    /**
     * A path variable could not be converted to its declared type — e.g. a request to
     * {@code /api/students/abc} where {@code {id}} is a {@code long}. The catch-all would
     * otherwise leak the Java type names ("Failed to convert value of type
     * 'java.lang.String' to required type 'long'") in a 500 body. We return a clean 400.
     *
     * <p>Parity note: Flask's {@code <int:student_id>} converter simply fails to match
     * such a URL and yields a 404; the frontend only ever sends numeric ids, so this
     * path is not reachable through the UI. 400 is used here as an honest client-error
     * signal while guaranteeing the {@code {"error": ...}} shape.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Invalid value in request URL."));
    }

    /**
     * Right URL, wrong HTTP verb (e.g. PATCH on a route that only allows PUT). Mapped to
     * the correct <b>405</b> instead of the catch-all's 500, still as {@code {"error": ...}}
     * so the frontend's unconditional {@code response.json()} keeps working.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorResponse("Method not allowed."));
    }

    /**
     * Body sent with an unsupported {@code Content-Type} (e.g. text/plain where JSON is
     * expected). Mapped to the correct <b>415</b> rather than a 500. The frontend always
     * sends the right content type, so this is a defensive backstop for the shape.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ErrorResponse("Unsupported content type."));
    }

    /**
     * A multipart upload that failed to PARSE (corrupt boundary, truncated stream, etc.)
     * as opposed to merely being oversized. The oversized case is a
     * {@link MaxUploadSizeExceededException} (a subclass) with its own, more specific
     * handler above, which Spring prefers — so that 413 message is unaffected. Everything
     * else lands here as a clean <b>400</b> instead of a leaky 500.
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipart(MultipartException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Could not read the uploaded files."));
    }

    /**
     * No route matched the requested URL. Two distinct exceptions can mean "no such
     * endpoint" depending on Spring's configuration, so we handle BOTH here:
     * <ul>
     *   <li>{@link NoResourceFoundException} — thrown by the resource-handling chain
     *       (the default in modern Spring Boot) when nothing serves the path;</li>
     *   <li>{@link NoHandlerFoundException} — thrown by the dispatcher when
     *       {@code throw-exception-if-no-handler-found} is enabled.</li>
     * </ul>
     *
     * <p><b>Why this exists:</b> without it, an unmatched route falls through to the
     * catch-all below and is reported as a <b>500</b> — telling the client "the server
     * broke" when the truth is "that URL does not exist". We map it to an honest
     * <b>404</b>, still as {@code {"error": ...}} so the frontend's unconditional
     * {@code response.json()} keeps working.
     */
    // @agent: exception-warden
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(Exception ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Not found."));
    }

    /**
     * Safety net for anything unforeseen — a bug, an unexpected library failure, etc.
     * Returning JSON (rather than Spring's default error page) keeps the frontend's
     * {@code response.json()} calls from choking.
     *
     * <p><b>Why the body is a FIXED string and NOT {@code ex.getMessage()}:</b> an
     * unexpected exception's message is an INTERNAL detail — it can carry SQL fragments,
     * file-system paths, library/class names, or other implementation specifics. Echoing
     * it to an API client is an information leak (and a security defect), so the client
     * only ever sees the generic {@code "Internal server error."}.
     *
     * <p><b>Why we log first:</b> a 500 that no one can see is a silent failure. We write
     * the full exception WITH its stack trace to the SERVER log at ERROR level, so the
     * detail we deliberately withhold from the client is still available to operators for
     * debugging. Log verbosely inward; respond opaquely outward.
     */
    // @agent: exception-warden
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception while processing request", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Internal server error."));
    }
}
