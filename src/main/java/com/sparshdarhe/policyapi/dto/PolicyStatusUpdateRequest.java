package com.sparshdarhe.policyapi.dto;

import com.sparshdarhe.policyapi.model.PolicyStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload for transitioning a policy's status
 * (e.g. ACTIVE -> RENEWED, ACTIVE -> CANCELLED).
 */
@Getter
@Setter
public class PolicyStatusUpdateRequest {

    @NotNull(message = "status is required")
    private PolicyStatus status;
}
