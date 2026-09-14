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

package com.lamprism.luxspec.security.authentication;

import com.lamprism.luxspec.context.ContextKey;
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;

import java.util.Objects;

/**
 * Represents one successfully authenticated actor and its effective grants.
 *
 * @author RollW
 */
public final class Authentication {
    public static final ContextKey<Authentication> CONTEXT_KEY = ContextKey.of(
            "security.authentication",
            Authentication.class
    );

    private final Subject subject;
    private final AuthorizationGrantSet grants;

    /**
     * Creates an authenticated actor with its effective concrete grants.
     *
     * @param subject the authenticated subject identity
     * @param grants  the immutable effective authorization grants
     */
    public Authentication(Subject subject, AuthorizationGrantSet grants) {
        Subject nonNullSubject = Objects.requireNonNull(subject, "subject");
        requireText(nonNullSubject.getType(), "subject.type");
        requireText(nonNullSubject.getId(), "subject.id");
        this.subject = nonNullSubject;
        this.grants = Objects.requireNonNull(grants, "grants");
    }

    /**
     * Returns the authenticated subject identity.
     *
     * @return the authenticated subject
     */
    public Subject subject() {
        return subject;
    }

    /**
     * Returns immutable effective authorization grants.
     *
     * @return the effective authorization grants
     */
    public AuthorizationGrantSet grants() {
        return grants;
    }

    private static String requireText(String value, String name) {
        String nonNullValue = Objects.requireNonNull(value, name);
        if (nonNullValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return nonNullValue;
    }
}
