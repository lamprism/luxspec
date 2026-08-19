/*
 * Copyright (C) Lamprism
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lamprism.luxspec.user.security.authorization;

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
 * Resolves user roles after profile and scope expansion.
 *
 * @author RollW
 */
public class UserRoleGrantResolverImpl implements UserRoleGrantResolver {
    private final Map<Role, AuthorizationGrantSet> grantsByRole;

    /**
     * Builds a resolver from role profile mappings, profile contributions, and a scope hierarchy.
     *
     * @param profilesByRole profiles assigned to each supported role
     * @param contributors   feature and default profile contributors
     * @param scopeHierarchy the assembled scope hierarchy
     */
    public UserRoleGrantResolverImpl(
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

    @Override
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
