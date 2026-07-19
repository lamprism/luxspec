package com.lamprism.luxspec.security.firewall;

import java.util.Objects;

/**
 * Reports a firewall rule failure that was converted to a default denial.
 *
 * @author RollW
 */
public final class FirewallRuleFailureEvent {
    private final String ruleType;

    /**
     * Creates safe failure metadata without retaining the request or throwable.
     *
     * @param ruleType the failing rule implementation type name
     */
    public FirewallRuleFailureEvent(String ruleType) {
        this.ruleType = Objects.requireNonNull(ruleType, "ruleType");
    }

    /**
     * Returns the failing rule implementation type name.
     *
     * @return the rule type name
     */
    public String getRuleType() {
        return ruleType;
    }
}
