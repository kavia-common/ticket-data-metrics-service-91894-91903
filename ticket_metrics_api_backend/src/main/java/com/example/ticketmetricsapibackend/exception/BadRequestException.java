package com.example.ticketmetricsapibackend.exception;

/**
 * PUBLIC_INTERFACE
 * Signals a 400 Bad Request scenario (e.g., missing file, invalid parameters).
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
