package com.sparshdarhe.policyapi.dto;

import com.sparshdarhe.policyapi.model.PolicyType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Inbound payload for creating a new policy (quotation).
 * Premium is calculated server-side and is intentionally NOT
 * part of this DTO, so a client cannot set its own premium.
 */
@Getter
@Setter
public class PolicyRequest {

    @NotBlank(message = "holderName is required")
    private String holderName;

    @NotBlank(message = "holderEmail is required")
    @Email(message = "holderEmail must be a valid email address")
    private String holderEmail;

    @NotNull(message = "policyType is required")
    private PolicyType policyType;

    @NotNull(message = "sumInsured is required")
    @DecimalMin(value = "1000.0", message = "sumInsured must be at least 1000")
    private BigDecimal sumInsured;

    @NotNull(message = "startDate is required")
    @FutureOrPresent(message = "startDate cannot be in the past")
    private LocalDate startDate;

    @NotNull(message = "endDate is required")
    private LocalDate endDate;
}
