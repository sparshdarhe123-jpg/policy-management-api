package com.sparshdarhe.policyapi.model;

/**
 * Product category for a policy. Premium calculation rules
 * differ by type (see PolicyService#calculatePremium).
 */
public enum PolicyType {
    HEALTH,
    LIFE,
    MOTOR,
    GROUP_HEALTH
}
