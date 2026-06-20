package com.sparshdarhe.policyapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparshdarhe.policyapi.dto.PolicyRequest;
import com.sparshdarhe.policyapi.dto.PolicyResponse;
import com.sparshdarhe.policyapi.dto.PolicyStatusUpdateRequest;
import com.sparshdarhe.policyapi.exception.PolicyNotFoundException;
import com.sparshdarhe.policyapi.model.PolicyStatus;
import com.sparshdarhe.policyapi.model.PolicyType;
import com.sparshdarhe.policyapi.service.PolicyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for PolicyController: verifies HTTP status codes,
 * JSON shape, and validation wiring, with the service layer
 * mocked out so this stays focused on the web layer.
 */
@WebMvcTest(PolicyController.class)
class PolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PolicyService policyService;

    @Test
    void createPolicy_returns201WithLocationAndBody() throws Exception {
        PolicyRequest request = new PolicyRequest();
        request.setHolderName("Sparsh Darhe");
        request.setHolderEmail("Sparsh@example.com");
        request.setPolicyType(PolicyType.HEALTH);
        request.setSumInsured(new BigDecimal("500000"));
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusMonths(12));

        PolicyResponse response = PolicyResponse.builder()
                .id(1L)
                .policyNumber("POL-123456")
                .holderName("Sparsh Darhe")
                .holderEmail("sparsh@example.com")
                .policyType(PolicyType.HEALTH)
                .status(PolicyStatus.QUOTED)
                .sumInsured(new BigDecimal("500000"))
                .premium(new BigDecimal("17500.00"))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        when(policyService.createPolicy(any(PolicyRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/policies/1"))
                .andExpect(jsonPath("$.policyNumber").value("POL-123456"))
                .andExpect(jsonPath("$.status").value("QUOTED"))
                .andExpect(jsonPath("$.premium").value(17500.00));
    }

    @Test
    void createPolicy_returns400WhenHolderEmailInvalid() throws Exception {
        PolicyRequest request = new PolicyRequest();
        request.setHolderName("Bad Email");
        request.setHolderEmail("not-an-email");
        request.setPolicyType(PolicyType.MOTOR);
        request.setSumInsured(new BigDecimal("100000"));
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusMonths(6));

        mockMvc.perform(post("/api/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.holderEmail").exists());
    }

    @Test
    void getPolicyById_returns404WhenMissing() throws Exception {
        when(policyService.getPolicyById(42L)).thenThrow(PolicyNotFoundException.forId(42L));

        mockMvc.perform(get("/api/v1/policies/{id}", 42L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No policy found with id: 42"));
    }

    @Test
    void updateStatus_returns200WithUpdatedStatus() throws Exception {
        PolicyStatusUpdateRequest request = new PolicyStatusUpdateRequest();
        request.setStatus(PolicyStatus.ACTIVE);

        PolicyResponse response = PolicyResponse.builder()
                .id(1L)
                .policyNumber("POL-123456")
                .holderName("Sparsh Darhe")
                .holderEmail("sparsh@example.com")
                .policyType(PolicyType.HEALTH)
                .status(PolicyStatus.ACTIVE)
                .sumInsured(new BigDecimal("500000"))
                .premium(new BigDecimal("17500.00"))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(12))
                .build();

        when(policyService.updateStatus(eq(1L), eq(PolicyStatus.ACTIVE))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/policies/{id}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void deletePolicy_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/policies/{id}", 1L))
                .andExpect(status().isNoContent());
    }
}
