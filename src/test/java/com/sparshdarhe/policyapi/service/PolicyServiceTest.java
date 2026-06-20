package com.sparshdarhe.policyapi.service;

import com.sparshdarhe.policyapi.dto.PolicyRequest;
import com.sparshdarhe.policyapi.dto.PolicyResponse;
import com.sparshdarhe.policyapi.exception.InvalidPolicyStateException;
import com.sparshdarhe.policyapi.exception.PolicyNotFoundException;
import com.sparshdarhe.policyapi.model.Policy;
import com.sparshdarhe.policyapi.model.PolicyStatus;
import com.sparshdarhe.policyapi.model.PolicyType;
import com.sparshdarhe.policyapi.repository.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PolicyService, the layer holding all business
 * rules: premium calculation, status transitions, and validation.
 * The repository is mocked so these tests run without a database.
 */
@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    private PolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService = new PolicyService(policyRepository);
    }

    @Test
    void createPolicy_savesWithCalculatedPremiumAndQuotedStatus() {
        PolicyRequest request = new PolicyRequest();
        request.setHolderName("Asha Verma");
        request.setHolderEmail("asha@example.com");
        request.setPolicyType(PolicyType.HEALTH);
        request.setSumInsured(new BigDecimal("500000"));
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusMonths(12));

        when(policyRepository.existsByPolicyNumber(any())).thenReturn(false);
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> {
            Policy p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        PolicyResponse response = policyService.createPolicy(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(PolicyStatus.QUOTED);
        assertThat(response.getPolicyNumber()).startsWith("POL-");
        // 500000 * 0.035 (HEALTH rate) * 1.0 (12-month duration factor) = 17500.00
        assertThat(response.getPremium()).isEqualByComparingTo("17500.00");

        ArgumentCaptor<Policy> captor = ArgumentCaptor.forClass(Policy.class);
        verify(policyRepository).save(captor.capture());
        assertThat(captor.getValue().getHolderEmail()).isEqualTo("asha@example.com");
    }

    @Test
    void createPolicy_rejectsEndDateBeforeStartDate() {
        PolicyRequest request = new PolicyRequest();
        request.setHolderName("Bad Dates");
        request.setHolderEmail("bad@example.com");
        request.setPolicyType(PolicyType.MOTOR);
        request.setSumInsured(new BigDecimal("100000"));
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> policyService.createPolicy(request))
                .isInstanceOf(InvalidPolicyStateException.class)
                .hasMessageContaining("endDate must be after startDate");

        verify(policyRepository, never()).save(any());
    }

    @Test
    void calculatePremium_appliesLongTermDiscountOverTwelveMonths() {
        BigDecimal sumInsured = new BigDecimal("1000000");
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = start.plusMonths(24); // 2-year term

        BigDecimal premium = policyService.calculatePremium(PolicyType.LIFE, sumInsured, start, end);

        // base: 1,000,000 * 0.015 * (24/12) = 30,000; with 5% discount = 28,500.00
        assertThat(premium).isEqualByComparingTo("28500.00");
    }

    @Test
    void calculatePremium_unknownTypeFallsBackToDefaultRate() {
        BigDecimal sumInsured = new BigDecimal("200000");
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = start.plusMonths(12);

        BigDecimal premium = policyService.calculatePremium(null, sumInsured, start, end);

        // default rate 0.03 * 200000 * 1.0 = 6000.00
        assertThat(premium).isEqualByComparingTo("6000.00");
    }

    @Test
    void getPolicyById_throwsWhenNotFound() {
        when(policyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyService.getPolicyById(99L))
                .isInstanceOf(PolicyNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateStatus_allowsQuotedToActive() {
        Policy existing = Policy.builder()
                .id(5L)
                .policyNumber("POL-100001")
                .status(PolicyStatus.QUOTED)
                .holderName("Test User")
                .holderEmail("t@example.com")
                .policyType(PolicyType.MOTOR)
                .sumInsured(new BigDecimal("100000"))
                .premium(new BigDecimal("4000.00"))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(12))
                .createdAt(LocalDate.now())
                .build();

        when(policyRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(policyRepository.save(any(Policy.class))).thenAnswer(inv -> inv.getArgument(0));

        PolicyResponse updated = policyService.updateStatus(5L, PolicyStatus.ACTIVE);

        assertThat(updated.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
    }

    @Test
    void updateStatus_rejectsInvalidTransitionFromLapsedToActive() {
        Policy existing = Policy.builder()
                .id(6L)
                .status(PolicyStatus.LAPSED)
                .policyNumber("POL-100002")
                .holderName("Test User")
                .holderEmail("t@example.com")
                .policyType(PolicyType.MOTOR)
                .sumInsured(new BigDecimal("100000"))
                .premium(new BigDecimal("4000.00"))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(12))
                .createdAt(LocalDate.now())
                .build();

        when(policyRepository.findById(6L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> policyService.updateStatus(6L, PolicyStatus.ACTIVE))
                .isInstanceOf(InvalidPolicyStateException.class)
                .hasMessageContaining("LAPSED")
                .hasMessageContaining("ACTIVE");

        verify(policyRepository, never()).save(any());
    }

    @Test
    void isValidTransition_coversFullLifecycleMatrix() {
        assertThat(policyService.isValidTransition(PolicyStatus.QUOTED, PolicyStatus.ACTIVE)).isTrue();
        assertThat(policyService.isValidTransition(PolicyStatus.QUOTED, PolicyStatus.RENEWED)).isFalse();
        assertThat(policyService.isValidTransition(PolicyStatus.ACTIVE, PolicyStatus.RENEWED)).isTrue();
        assertThat(policyService.isValidTransition(PolicyStatus.ACTIVE, PolicyStatus.LAPSED)).isTrue();
        assertThat(policyService.isValidTransition(PolicyStatus.CANCELLED, PolicyStatus.ACTIVE)).isFalse();
        assertThat(policyService.isValidTransition(PolicyStatus.LAPSED, PolicyStatus.ACTIVE)).isFalse();
    }
}
