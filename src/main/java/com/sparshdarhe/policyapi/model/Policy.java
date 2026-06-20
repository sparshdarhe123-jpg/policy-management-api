package com.sparshdarhe.policyapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Core entity representing an insurance policy.
 *
 * Maps to the "policies" table. Premium and status are managed
 * via PolicyService rather than set directly, to keep business
 * rules centralized.
 */
@Entity
@Table(name = "policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String policyNumber;

    @Column(nullable = false)
    private String holderName;

    @Column(nullable = false)
    private String holderEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PolicyType policyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PolicyStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal sumInsured;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal premium;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private LocalDate createdAt;
}
