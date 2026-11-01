package com.sentinel.exception;

/**
 * Thrown when something goes wrong while processing/scoring a transaction
 * (e.g. bad data, DB failure during risk evaluation).
 */
public class FraudDetectionException extends Exception {
    public FraudDetectionException(String message) {
        super(message);
    }

    public FraudDetectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
