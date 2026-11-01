package com.sentinel.exception;

/**
 * Thrown when a user tries to perform an action their Role does not permit.
 */
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
