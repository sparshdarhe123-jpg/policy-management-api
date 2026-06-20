package com.sparshdarhe.policyapi.model;

/**
 * Represents the lifecycle state of an insurance policy.
 *
 * Lifecycle: QUOTED -> ACTIVE -> (RENEWED | LAPSED | CANCELLED)
 */
public enum PolicyStatus {
    QUOTED,
    ACTIVE,
    RENEWED,
    LAPSED,
    CANCELLED
}
