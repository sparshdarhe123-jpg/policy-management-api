package com.sparshdarhe.policyapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error body returned by the global exception handler.
 * `fieldErrors` is populated only for validation failures.
 */
@Getter
@AllArgsConstructor
public class ErrorResponse {
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private Map<String, String> fieldErrors;
}
