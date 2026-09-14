package com.lamprism.luxspec.config.policy;

/**
 * Identifies when a policy context is evaluated in the configuration pipeline.
 *
 * @author RollW
 */
public enum ConfigPolicyPhase {
    /**
     * A definition-level fallback or Source mutation operation is being evaluated.
     */
    OPERATION,
    /**
     * A Source candidate is being evaluated before its raw entry is read.
     */
    BEFORE_SOURCE_READ,
    /**
     * A Source candidate is being evaluated after its raw entry has been read.
     */
    AFTER_SOURCE_READ
}
