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

package com.lamprism.luxspec.security.token;

import com.lamprism.luxspec.AuthErrorCode;
import com.lamprism.luxspec.security.authentication.AuthenticationException;
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import com.lamprism.luxspec.security.authorization.AuthorizationScopeHierarchy;

import java.util.Objects;

/**
 * Determines effective grants from issued and current grant sets.
 *
 * @author RollW
 */
@FunctionalInterface
public interface TokenGrantPolicy {
    /**
     * Returns a policy that intersects issued and current grants.
     *
     * @return the intersection policy
     */
    static TokenGrantPolicy intersection() {
        return AuthorizationGrantSet::intersect;
    }

    /**
     * Expands the issued scopes before intersecting them with current effective grants.
     *
     * @param hierarchy       the scope hierarchy
     * @param requireNonEmpty whether an empty issued grant set is rejected
     * @return the expanding intersection policy
     */
    static TokenGrantPolicy expandedIntersection(AuthorizationScopeHierarchy hierarchy, boolean requireNonEmpty) {
        AuthorizationScopeHierarchy nonNullHierarchy = Objects.requireNonNull(hierarchy, "hierarchy");
        return (issued, current) -> {
            if (requireNonEmpty && issued.getScopes().isEmpty()) {
                throw new AuthenticationException(AuthErrorCode.INVALID_TOKEN, "Token grants are empty");
            }
            return AuthorizationGrantSet.expanded(issued.getScopes(), nonNullHierarchy).intersect(current);
        };
    }

    /**
     * Computes the effective grant set.
     *
     * @param issued  the grants recorded in the token or session
     * @param current the actor's current effective grants
     * @return the effective grants
     */
    AuthorizationGrantSet apply(AuthorizationGrantSet issued, AuthorizationGrantSet current);
}
