package com.lamprism.luxspec.security.authentication;

import com.lamprism.luxspec.context.ContextKey;

/**
 * Defines typed ExecutionContext keys owned by the security domain.
 *
 * @author RollW
 */
public final class SecurityContextKeys {
    /**
     * Identifies the effective Luxspec Authentication for an execution scope.
     */
    public static final ContextKey<Authentication> AUTHENTICATION = ContextKey.of(
            "security.authentication",
            Authentication.class
    );

    private SecurityContextKeys() {
    }
}
