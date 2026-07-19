package com.lamprism.luxspec.security.authorization;

import java.util.Objects;

/**
 * A stable concrete authority identifier.
 *
 * @author RollW
 */
public final class AuthorizationScope {
    private final String name;

    /**
     * Creates a canonical concrete authorization scope.
     *
     * @param name the canonical scope name
     */
    public AuthorizationScope(String name) {
        this.name = Objects.requireNonNull(name, "name");
        ScopeNames.requireCanonical(name);
    }

    /**
     * Returns the stable canonical scope name.
     *
     * @return the scope name
     */
    public String name() {
        return name;
    }

    /**
     * Creates one canonical authorization scope.
     *
     * @param name the canonical scope name
     * @return the authorization scope
     */
    public static AuthorizationScope of(String name) {
        return new AuthorizationScope(name);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AuthorizationScope scope)) {
            return false;
        }
        return name.equals(scope.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
