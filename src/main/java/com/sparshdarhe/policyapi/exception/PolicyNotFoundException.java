package com.sparshdarhe.policyapi.exception;

/**
 * Thrown when a policy lookup by id or policy number fails.
 * Translated to HTTP 404 by GlobalExceptionHandler.
 */
public class PolicyNotFoundException extends RuntimeException {

    public PolicyNotFoundException(String message) {
        super(message);
    }

    public static PolicyNotFoundException forId(Long id) {
        return new PolicyNotFoundException("No policy found with id: " + id);
    }

    public static PolicyNotFoundException forPolicyNumber(String policyNumber) {
        return new PolicyNotFoundException("No policy found with policyNumber: " + policyNumber);
    }
}
