package com.sparshdarhe.policyapi.repository;

import com.sparshdarhe.policyapi.model.Policy;
import com.sparshdarhe.policyapi.model.PolicyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    Optional<Policy> findByPolicyNumber(String policyNumber);

    List<Policy> findByStatus(PolicyStatus status);

    List<Policy> findByHolderEmailIgnoreCase(String holderEmail);

    boolean existsByPolicyNumber(String policyNumber);
}
