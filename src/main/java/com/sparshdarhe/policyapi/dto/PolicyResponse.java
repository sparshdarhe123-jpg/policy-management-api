package com.sparshdarhe.policyapi.dto;

import com.sparshdarhe.policyapi.model.PolicyStatus;
import com.sparshdarhe.policyapi.model.PolicyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Outbound representation of a policy. Kept separate from the
 * Policy entity so internal persistence fields (e.g. createdAt
 * formatting) can evolve without breaking API consumers.
 */
@Getter
@Builder
@AllArgsConstructor
public class PolicyResponse {
    private Long id;
    private String policyNumber;
    private String holderName;
    private String holderEmail;
    private PolicyType policyType;
    private PolicyStatus status;
    private BigDecimal sumInsured;
    private BigDecimal premium;
    private LocalDate startDate;
    private LocalDate endDate;
}
