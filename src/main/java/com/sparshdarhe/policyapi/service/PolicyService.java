package com.sparshdarhe.policyapi.service;

import com.sparshdarhe.policyapi.dto.PolicyRequest;
import com.sparshdarhe.policyapi.dto.PolicyResponse;
import com.sparshdarhe.policyapi.exception.InvalidPolicyStateException;
import com.sparshdarhe.policyapi.exception.PolicyNotFoundException;
import com.sparshdarhe.policyapi.model.Policy;
import com.sparshdarhe.policyapi.model.PolicyStatus;
import com.sparshdarhe.policyapi.model.PolicyType;
import com.sparshdarhe.policyapi.repository.PolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Encapsulates all policy business rules: premium calculation,
 * policy number generation, and valid lifecycle transitions.
 * Controllers stay thin and only translate HTTP <-> DTOs.
 */
@Service
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final SecureRandom random = new SecureRandom();

    // Base annual rate applied to sum insured, per product type.
    // Real systems would source this from a rating table / pricing
    // engine; a fixed map is intentionally simple here.
    private static final Map<PolicyType, BigDecimal> BASE_RATE = new EnumMap<>(PolicyType.class);
    static {
        BASE_RATE.put(PolicyType.HEALTH, new BigDecimal("0.035"));
        BASE_RATE.put(PolicyType.LIFE, new BigDecimal("0.015"));
        BASE_RATE.put(PolicyType.MOTOR, new BigDecimal("0.04"));
        BASE_RATE.put(PolicyType.GROUP_HEALTH, new BigDecimal("0.028"));
    }

    // Allowed forward transitions in the policy lifecycle.
    private static final Map<PolicyStatus, Set<PolicyStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(PolicyStatus.class);
    static {
        ALLOWED_TRANSITIONS.put(PolicyStatus.QUOTED, EnumSet.of(PolicyStatus.ACTIVE, PolicyStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(PolicyStatus.ACTIVE, EnumSet.of(PolicyStatus.RENEWED, PolicyStatus.LAPSED, PolicyStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(PolicyStatus.RENEWED, EnumSet.of(PolicyStatus.LAPSED, PolicyStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(PolicyStatus.LAPSED, EnumSet.noneOf(PolicyStatus.class));
        ALLOWED_TRANSITIONS.put(PolicyStatus.CANCELLED, EnumSet.noneOf(PolicyStatus.class));
    }

    public PolicyService(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @Transactional
    public PolicyResponse createPolicy(PolicyRequest request) {
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new InvalidPolicyStateException("endDate must be after startDate");
        }

        BigDecimal premium = calculatePremium(
                request.getPolicyType(),
                request.getSumInsured(),
                request.getStartDate(),
                request.getEndDate()
        );

        Policy policy = Policy.builder()
                .policyNumber(generateUniquePolicyNumber())
                .holderName(request.getHolderName())
                .holderEmail(request.getHolderEmail())
                .policyType(request.getPolicyType())
                .status(PolicyStatus.QUOTED)
                .sumInsured(request.getSumInsured())
                .premium(premium)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .createdAt(LocalDate.now())
                .build();

        Policy saved = policyRepository.save(policy);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PolicyResponse getPolicyById(Long id) {
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> PolicyNotFoundException.forId(id));
        return toResponse(policy);
    }

    @Transactional(readOnly = true)
    public PolicyResponse getPolicyByNumber(String policyNumber) {
        Policy policy = policyRepository.findByPolicyNumber(policyNumber)
                .orElseThrow(() -> PolicyNotFoundException.forPolicyNumber(policyNumber));
        return toResponse(policy);
    }

    @Transactional(readOnly = true)
    public List<PolicyResponse> getAllPolicies(PolicyStatus statusFilter) {
        List<Policy> policies = (statusFilter == null)
                ? policyRepository.findAll()
                : policyRepository.findByStatus(statusFilter);

        return policies.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PolicyResponse updateStatus(Long id, PolicyStatus newStatus) {
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> PolicyNotFoundException.forId(id));

        PolicyStatus current = policy.getStatus();
        if (!isValidTransition(current, newStatus)) {
            throw new InvalidPolicyStateException(
                    String.format("Cannot transition policy from %s to %s", current, newStatus));
        }

        policy.setStatus(newStatus);
        Policy saved = policyRepository.save(policy);
        return toResponse(saved);
    }

    @Transactional
    public void deletePolicy(Long id) {
        if (!policyRepository.existsById(id)) {
            throw PolicyNotFoundException.forId(id);
        }
        policyRepository.deleteById(id);
    }

    // ---- Business logic helpers ----

    /**
     * Premium = sumInsured * baseRate(type) * durationFactor.
     * durationFactor pro-rates the annual base rate by policy
     * term length in months (minimum 1 month), then applies a
     * small long-term discount for terms over 12 months.
     */
    BigDecimal calculatePremium(PolicyType type, BigDecimal sumInsured, LocalDate start, LocalDate end) {
        BigDecimal baseRate = BASE_RATE.getOrDefault(type, new BigDecimal("0.03"));

        long months = Math.max(1, ChronoUnit.MONTHS.between(start, end));
        BigDecimal durationFactor = BigDecimal.valueOf(months)
                .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);

        BigDecimal premium = sumInsured.multiply(baseRate).multiply(durationFactor);

        if (months > 12) {
            // 5% discount for multi-year terms
            premium = premium.multiply(new BigDecimal("0.95"));
        }

        return premium.setScale(2, RoundingMode.HALF_UP);
    }

    boolean isValidTransition(PolicyStatus from, PolicyStatus to) {
        return ALLOWED_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(PolicyStatus.class)).contains(to);
    }

    private String generateUniquePolicyNumber() {
        String candidate;
        do {
            candidate = "POL-" + (100000 + random.nextInt(900000));
        } while (policyRepository.existsByPolicyNumber(candidate));
        return candidate;
    }

    private PolicyResponse toResponse(Policy policy) {
        return PolicyResponse.builder()
                .id(policy.getId())
                .policyNumber(policy.getPolicyNumber())
                .holderName(policy.getHolderName())
                .holderEmail(policy.getHolderEmail())
                .policyType(policy.getPolicyType())
                .status(policy.getStatus())
                .sumInsured(policy.getSumInsured())
                .premium(policy.getPremium())
                .startDate(policy.getStartDate())
                .endDate(policy.getEndDate())
                .build();
    }
}
