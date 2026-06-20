package com.sparshdarhe.policyapi.controller;

import com.sparshdarhe.policyapi.dto.PolicyRequest;
import com.sparshdarhe.policyapi.dto.PolicyResponse;
import com.sparshdarhe.policyapi.dto.PolicyStatusUpdateRequest;
import com.sparshdarhe.policyapi.model.PolicyStatus;
import com.sparshdarhe.policyapi.service.PolicyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * REST API for managing insurance policies.
 * Base path: /api/v1/policies
 */
@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @PostMapping
    public ResponseEntity<PolicyResponse> createPolicy(@Valid @RequestBody PolicyRequest request) {
        PolicyResponse created = policyService.createPolicy(request);
        return ResponseEntity.created(URI.create("/api/v1/policies/" + created.getId())).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PolicyResponse> getPolicyById(@PathVariable Long id) {
        return ResponseEntity.ok(policyService.getPolicyById(id));
    }

    @GetMapping("/number/{policyNumber}")
    public ResponseEntity<PolicyResponse> getPolicyByNumber(@PathVariable String policyNumber) {
        return ResponseEntity.ok(policyService.getPolicyByNumber(policyNumber));
    }

    @GetMapping
    public ResponseEntity<List<PolicyResponse>> getAllPolicies(
            @RequestParam(required = false) PolicyStatus status) {
        return ResponseEntity.ok(policyService.getAllPolicies(status));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PolicyResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody PolicyStatusUpdateRequest request) {
        return ResponseEntity.ok(policyService.updateStatus(id, request.getStatus()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePolicy(@PathVariable Long id) {
        policyService.deletePolicy(id);
        return ResponseEntity.noContent().build();
    }
}
