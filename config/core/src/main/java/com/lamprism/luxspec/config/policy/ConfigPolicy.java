package com.lamprism.luxspec.config.policy;

import java.util.Objects;

/**
 * Evaluates one operation-aware configuration constraint.
 *
 * @author RollW
 */
@FunctionalInterface
public interface ConfigPolicy {
    /**
     * Evaluates the policy against immutable operation metadata.
     *
     * @param context the policy context
     * @return the explicit policy decision
     */
    ConfigPolicyDecision evaluate(ConfigPolicyContext context);

    /**
     * Composes this policy with another policy.
     *
     * @param other the additional policy
     * @return the deterministic composite policy
     */
    default ConfigPolicy and(ConfigPolicy other) {
        return ConfigPolicies.allOf(this, Objects.requireNonNull(other, "other"));
    }

    /**
     * Returns an allow-all policy.
     *
     * @return the allow-all policy
     */
    static ConfigPolicy allow() {
        return context -> ConfigPolicyDecision.ALLOW;
    }
}
