package com.lamprism.luxspec.config.policy;

import com.lamprism.luxspec.config.ConfigKey;

/**
 * Indicates that a configuration operation was rejected by its policy.
 *
 * @author RollW
 */
public final class ConfigPolicyException extends IllegalArgumentException {
    /**
     * Creates a policy violation for one binding and operation.
     *
     * @param key       the affected complete key
     * @param operation the rejected operation
     * @param decision  the policy decision
     */
    public ConfigPolicyException(
            ConfigKey key,
            ConfigPolicyOperation operation,
            ConfigPolicyDecision decision
    ) {
        super("Configuration policy rejected " + operation + " for key: " + key.getValue()
                + " with decision: " + decision);
    }
}
