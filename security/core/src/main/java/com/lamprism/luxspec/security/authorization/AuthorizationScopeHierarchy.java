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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Expands declared authorization scopes through an immutable validated implication hierarchy.
 *
 * @author RollW
 */
public final class AuthorizationScopeHierarchy {
    private final Map<AuthorizationScope, Set<AuthorizationScope>> descendants;

    private AuthorizationScopeHierarchy(Map<AuthorizationScope, Set<AuthorizationScope>> descendants) {
        this.descendants = Map.copyOf(descendants);
    }

    /**
     * Creates an empty hierarchy that accepts no undeclared scopes.
     *
     * @return the empty hierarchy
     */
    public static AuthorizationScopeHierarchy empty() {
        return builder().build();
    }

    /**
     * Creates a builder for explicit scope definitions and implication edges.
     *
     * @return the hierarchy builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Expands direct grants into immutable effective grants.
     *
     * @param directScopes the directly assigned scopes
     * @return direct and implied scopes
     */
    public Set<AuthorizationScope> expand(Collection<AuthorizationScope> directScopes) {
        Set<AuthorizationScope> effective = new LinkedHashSet<>();
        for (AuthorizationScope scope : directScopes) {
            AuthorizationScope nonNullScope = Objects.requireNonNull(scope, "scope");
            Set<AuthorizationScope> expanded = descendants.get(nonNullScope);
            if (expanded == null) {
                throw new IllegalArgumentException("Authorization scope is not declared in this hierarchy");
            }
            effective.addAll(expanded);
        }
        return Set.copyOf(effective);
    }

    /**
     * Builds one immutable scope hierarchy.
     */
    public static final class Builder {
        private final Map<AuthorizationScope, Set<AuthorizationScope>> edges = new LinkedHashMap<>();

        /**
         * Declares one scope that may be granted or implied.
         *
         * @param scope the scope to declare
         * @return this builder
         */
        public Builder declare(AuthorizationScope scope) {
            AuthorizationScope nonNullScope = Objects.requireNonNull(scope, "scope");
            if (edges.putIfAbsent(nonNullScope, new LinkedHashSet<>()) != null) {
                throw new IllegalArgumentException("Authorization scope is already declared");
            }
            return this;
        }

        /**
         * Declares that a granted scope also grants one descendant scope.
         *
         * @param ancestor   the directly granted ancestor scope
         * @param descendant the implied descendant scope
         * @return this builder
         */
        public Builder imply(AuthorizationScope ancestor, AuthorizationScope descendant) {
            AuthorizationScope nonNullAncestor = Objects.requireNonNull(ancestor, "ancestor");
            AuthorizationScope nonNullDescendant = Objects.requireNonNull(descendant, "descendant");
            if (nonNullAncestor.equals(nonNullDescendant)) {
                throw new IllegalArgumentException("Authorization scope must not imply itself");
            }
            Set<AuthorizationScope> children = edges.get(nonNullAncestor);
            if (children == null || !edges.containsKey(nonNullDescendant)) {
                throw new IllegalArgumentException("Authorization hierarchy references an undeclared scope");
            }
            if (!children.add(nonNullDescendant)) {
                throw new IllegalArgumentException("Authorization hierarchy contains a duplicate implication");
            }
            return this;
        }

        /**
         * Validates and creates immutable transitive scope expansions.
         *
         * @return the immutable hierarchy
         */
        public AuthorizationScopeHierarchy build() {
            Map<AuthorizationScope, Set<AuthorizationScope>> expanded = new LinkedHashMap<>();
            for (AuthorizationScope scope : edges.keySet()) {
                expanded.put(scope, expand(scope, new LinkedHashSet<>()));
            }
            return new AuthorizationScopeHierarchy(expanded);
        }

        private Set<AuthorizationScope> expand(AuthorizationScope scope, Set<AuthorizationScope> visiting) {
            if (!visiting.add(scope)) {
                throw new IllegalArgumentException("Authorization hierarchy contains a cycle");
            }
            Set<AuthorizationScope> result = new LinkedHashSet<>();
            result.add(scope);
            for (AuthorizationScope descendant : edges.get(scope)) {
                result.addAll(expand(descendant, visiting));
            }
            visiting.remove(scope);
            return Set.copyOf(result);
        }
    }
}
