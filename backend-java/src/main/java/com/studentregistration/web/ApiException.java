package com.studentregistration.web;

import org.springframework.http.HttpStatus;

/**
 * ApiException — a single, deliberate exception type that our service layer throws
 * to signal "return this HTTP status with this message to the client".
 *
 * <p>The Python backend returned errors inline, e.g.
 * {@code return jsonify({"error": "..."}), 400}. Java's idiomatic equivalent is to
 * THROW from deep in the business logic and CATCH centrally. Every ApiException
 * carries the two things a Flask error return carried — a status code and a human
 * message — and {@link GlobalExceptionHandler} turns it into the exact
 * {@code {"error": "..."}} JSON body the frontend expects.
 *
 * <p>It extends {@link RuntimeException} (an "unchecked" exception) so we don't have
 * to declare {@code throws} on every method in the call chain.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    // --- Small factory helpers so call sites read like the Flask returns --------

    /** 400 Bad Request — failed validation. */
    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    /** 404 Not Found — the student or document doesn't exist. */
    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }
}
