package com.lamprism.luxspec.config.policy;

/**
 * Describes the result of evaluating one configuration policy.
 *
 * @author RollW
 */
public enum ConfigPolicyDecision {
    /**
     * Permit the current operation or source candidate.
     */
    ALLOW,
    /**
     * Ignore the current Source candidate; this decision is not valid for fallback evaluation.
     */
    SKIP,
    /**
     * Reject the requested operation.
     */
    DENY,
    /**
     * Stop lower-layer or fallback evaluation.
     */
    STOP
}
