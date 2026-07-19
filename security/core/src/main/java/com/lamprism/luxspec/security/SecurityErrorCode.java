package com.lamprism.luxspec.security;

import com.lamprism.luxspec.ErrorCode;

/**
 * Defines stable security-domain errors not owned by authentication credentials.
 *
 * @author RollW
 */
public enum SecurityErrorCode implements ErrorCode {
    /**
     * A configured firewall rule failed unexpectedly.
     */
    FIREWALL_RULE_FAILURE("security:firewall-rule-failure"),
    /**
     * A subject was denied access to a resource action.
     */
    RESOURCE_ACCESS_DENIED("security:resource-access-denied"),
    /**
     * An action does not apply to the referenced resource type.
     */
    UNSUPPORTED_RESOURCE_ACTION("security:unsupported-resource-action");

    private final String code;

    SecurityErrorCode(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
