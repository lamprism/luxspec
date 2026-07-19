package com.lamprism.luxspec.user.security;

import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import com.lamprism.luxspec.security.authorization.AuthorizationScope;
import com.lamprism.luxspec.security.authorization.AuthorizationScopeHierarchy;
import com.lamprism.luxspec.user.Role;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Resolves user roles into immutable effective authorization grants after profile and scope expansion.
 *
 * @author RollW
 */
public final class UserRoleGrantResolver {
    private final Map<Role, AuthorizationGrantSet> grantsByRole;

    /**
     * Builds a resolver from Role profile mappings, profile contributions, and a scope hierarchy.
     *
     * @param profilesByRole profiles assigned to each supported role
     * @param contributors feature and default profile contributors
     * @param scopeHierarchy the assembled scope hierarchy
     */
    public UserRoleGrantResolver(
            Map<Role, ? extends Collection<AuthorizationProfile>> profilesByRole,
            Iterable<? extends AuthorizationProfileContributor> contributors,
            AuthorizationScopeHierarchy scopeHierarchy
    ) {
        AuthorizationProfileRegistry registry = new AuthorizationProfileRegistry();
        for (AuthorizationProfileContributor contributor : Objects.requireNonNull(contributors, "contributors")) {
            Objects.requireNonNull(contributor, "contributor").contribute(registry);
        }
        registry.validate();
        this.grantsByRole = buildGrants(profilesByRole, registry, scopeHierarchy);
    }

    /**
     * Resolves roles into one immutable effective grant set.
     *
     * @param roles the assigned user roles
     * @return the expanded effective grants
     */
    public AuthorizationGrantSet resolve(Collection<Role> roles) {
        Set<AuthorizationScope> scopes = new LinkedHashSet<>();
        for (Role role : Objects.requireNonNull(roles, "roles")) {
            AuthorizationGrantSet grants = grantsByRole.get(Objects.requireNonNull(role, "role"));
            if (grants == null) {
                throw new IllegalArgumentException("User role has no authorization profile mapping");
            }
            scopes.addAll(grants.getScopes());
        }
        return AuthorizationGrantSet.of(scopes);
    }

    private static Map<Role, AuthorizationGrantSet> buildGrants(
            Map<Role, ? extends Collection<AuthorizationProfile>> profilesByRole,
            AuthorizationProfileRegistry registry,
            AuthorizationScopeHierarchy scopeHierarchy
    ) {
        Map<Role, AuthorizationGrantSet> grants = new LinkedHashMap<>();
        for (Map.Entry<Role, ? extends Collection<AuthorizationProfile>> entry : Objects.requireNonNull(profilesByRole, "profilesByRole").entrySet()) {
            Role role = Objects.requireNonNull(entry.getKey(), "role");
            Set<AuthorizationScope> scopes = new LinkedHashSet<>();
            for (AuthorizationProfile profile : Objects.requireNonNull(entry.getValue(), "profiles")) {
                scopes.addAll(registry.resolve(Objects.requireNonNull(profile, "profile")));
            }
            grants.put(role, AuthorizationGrantSet.expanded(scopes, Objects.requireNonNull(scopeHierarchy, "scopeHierarchy")));
        }
        return Map.copyOf(grants);
    }
}
