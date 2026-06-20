package com.sparshdarhe.policyapi.exception;

/**
 * Thrown when a requested policy status transition violates
 * the allowed lifecycle (see PolicyService#isValidTransition).
 * Translated to HTTP 409 by GlobalExceptionHandler.
 */
public class InvalidPolicyStateException extends RuntimeException {

    public InvalidPolicyStateException(String message) {
        super(message);
    }
}
