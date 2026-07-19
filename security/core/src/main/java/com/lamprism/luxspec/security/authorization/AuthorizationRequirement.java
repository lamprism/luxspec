package com.lamprism.luxspec.security.authorization;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Defines the baseline grants required before resource-instance authorization.
 *
 * @author RollW
 */
public final class AuthorizationRequirement {
    private enum Kind {
        NONE,
        ANY,
        ALL
    }

    private static final AuthorizationRequirement NONE = new AuthorizationRequirement(Kind.NONE, Set.of());

    private final Kind kind;
    private final Set<AuthorizationScope> scopes;

    private AuthorizationRequirement(Kind kind, Set<AuthorizationScope> scopes) {
        this.kind = kind;
        this.scopes = scopes;
    }

    /**
     * Returns the requirement that needs no baseline grant.
     *
     * @return the shared no-grant requirement
     */
    public static AuthorizationRequirement none() {
        return NONE;
    }

    /**
     * Requires one concrete authorization scope.
     *
     * @param scope the required scope
     * @return the single-scope requirement
     */
    public static AuthorizationRequirement requires(AuthorizationScope scope) {
        return requiresAll(Set.of(scope));
    }

    /**
     * Requires at least one scope from the supplied set.
     *
     * @param scopes the alternative required scopes
     * @return the any-scope requirement
     */
    public static AuthorizationRequirement requiresAny(Collection<AuthorizationScope> scopes) {
        return new AuthorizationRequirement(Kind.ANY, copyScopes(scopes));
    }

    /**
     * Requires every scope from the supplied set.
     *
     * @param scopes the required scopes
     * @return the all-scope requirement
     */
    public static AuthorizationRequirement requiresAll(Collection<AuthorizationScope> scopes) {
        return new AuthorizationRequirement(Kind.ALL, copyScopes(scopes));
    }

    /**
     * Reports whether the supplied grants satisfy this baseline requirement.
     *
     * @param grants the effective authorization grants
     * @return whether the requirement is satisfied
     */
    public boolean isSatisfiedBy(AuthorizationGrantSet grants) {
        Objects.requireNonNull(grants, "grants");
        if (kind == Kind.NONE) {
            return true;
        }
        if (kind == Kind.ANY) {
            for (AuthorizationScope scope : scopes) {
                if (grants.contains(scope)) {
                    return true;
                }
            }
            return false;
        }
        for (AuthorizationScope scope : scopes) {
            if (!grants.contains(scope)) {
                return false;
            }
        }
        return true;
    }

    private static Set<AuthorizationScope> copyScopes(Collection<AuthorizationScope> scopes) {
        Objects.requireNonNull(scopes, "scopes");
        Set<AuthorizationScope> result = new LinkedHashSet<>();
        for (AuthorizationScope scope : scopes) {
            result.add(Objects.requireNonNull(scope, "scope"));
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Authorization requirement scopes must not be empty");
        }
        if (result.size() != scopes.size()) {
            throw new IllegalArgumentException("Authorization requirement scopes must not contain duplicates");
        }
        return Set.copyOf(result);
    }
}
