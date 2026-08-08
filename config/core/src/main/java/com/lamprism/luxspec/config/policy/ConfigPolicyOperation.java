package com.lamprism.luxspec.config.policy;

/**
 * Identifies the configuration operation being evaluated by a policy.
 *
 * @author RollW
 */
public enum ConfigPolicyOperation {
    /**
     * Evaluate one Source candidate during effective resolution.
     */
    RESOLVE_SOURCE,
    /**
     * Evaluate Spec default fallback.
     */
    RESOLVE_FALLBACK,
    /**
     * Evaluate a typed set operation.
     */
    SET,
    /**
     * Evaluate a typed remove operation.
     */
    REMOVE
}
