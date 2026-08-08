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

package com.lamprism.luxspec.security.authorization;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Holds the immutable effective concrete scopes of an Authentication.
 *
 * @author RollW
 */
public final class AuthorizationGrantSet {
    private final Set<AuthorizationScope> scopes;

    private AuthorizationGrantSet(Collection<AuthorizationScope> scopes) {
        this.scopes = Set.copyOf(new LinkedHashSet<>(scopes));
    }

    /**
     * Creates an immutable set of concrete authorization scopes.
     *
     * @param scopes the effective concrete scopes
     * @return the immutable grant set
     */
    public static AuthorizationGrantSet of(Collection<AuthorizationScope> scopes) {
        return new AuthorizationGrantSet(scopes);
    }

    /**
     * Creates an immutable effective grant set by expanding direct scopes once.
     *
     * @param directScopes the direct assigned scopes
     * @param hierarchy    the assembled scope hierarchy
     * @return the effective grant set
     */
    public static AuthorizationGrantSet expanded(
            Collection<AuthorizationScope> directScopes,
            AuthorizationScopeHierarchy hierarchy
    ) {
        return new AuthorizationGrantSet(Objects.requireNonNull(hierarchy, "hierarchy").expand(directScopes));
    }

    /**
     * Reports whether this grant set contains a concrete scope.
     *
     * @param scope the concrete scope to check
     * @return whether the scope is granted
     */
    public boolean contains(AuthorizationScope scope) {
        return scopes.contains(scope);
    }

    /**
     * Returns the grants shared by this set and one authorization ceiling.
     *
     * @param ceiling the maximum grants that may remain effective
     * @return the immutable grant intersection
     */
    public AuthorizationGrantSet intersect(AuthorizationGrantSet ceiling) {
        AuthorizationGrantSet nonNullCeiling = Objects.requireNonNull(ceiling, "ceiling");
        LinkedHashSet<AuthorizationScope> intersection = new LinkedHashSet<>(scopes);
        intersection.retainAll(nonNullCeiling.scopes);
        return new AuthorizationGrantSet(intersection);
    }

    /**
     * Returns immutable concrete scopes.
     *
     * @return the effective concrete scopes
     */
    public Set<AuthorizationScope> getScopes() {
        return scopes;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AuthorizationGrantSet grantSet)) {
            return false;
        }
        return scopes.equals(grantSet.scopes);
    }

    @Override
    public int hashCode() {
        return scopes.hashCode();
    }
}
