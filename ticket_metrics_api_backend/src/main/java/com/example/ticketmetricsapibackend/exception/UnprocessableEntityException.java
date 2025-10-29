package com.example.ticketmetricsapibackend.exception;

/**
 * PUBLIC_INTERFACE
 * Signals a 422 Unprocessable Entity scenario (e.g., invalid/unreadable Excel format).
 */
public class UnprocessableEntityException extends RuntimeException {
    public UnprocessableEntityException(String message) {
        super(message);
    }

    public UnprocessableEntityException(String message, Throwable cause) {
        super(message, cause);
    }
}
