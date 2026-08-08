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
