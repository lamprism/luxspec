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

import java.util.Objects;

/**
 * Identifies one credential Java type for exact authenticator dispatch.
 *
 * @param <C> the credential type
 * @author RollW
 */
public final class CredentialType<C extends Credentials> {
    private final String name;
    private final Class<C> credentialsType;

    private CredentialType(String name, Class<C> credentialsType) {
        this.name = name;
        this.credentialsType = credentialsType;
    }

    /**
     * Creates one canonical credential-type identity.
     *
     * @param name            the canonical credential type name
     * @param credentialsType the exact Java credentials type
     * @param <C>             the credential value type
     * @return the immutable credential type
     */
    public static <C extends Credentials> CredentialType<C> of(
            String name,
            Class<C> credentialsType
    ) {
        requireCanonical(Objects.requireNonNull(name, "name"));
        return new CredentialType<>(name, Objects.requireNonNull(credentialsType, "credentialsType"));
    }

    /**
     * Returns the canonical credential type name.
     *
     * @return the credential type name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the exact Java credentials type used for registry dispatch.
     *
     * @return the credentials Java type
     */
    public Class<C> getCredentialsType() {
        return credentialsType;
    }

    private static void requireCanonical(String value) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Credential type name must not be empty");
        }
        boolean segmentStart = true;
        boolean previousHyphen = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == ':') {
                if (segmentStart || previousHyphen) {
                    throw new IllegalArgumentException("Credential type name contains an invalid segment");
                }
                segmentStart = true;
                continue;
            }
            if (segmentStart) {
                if (character < 'a' || character > 'z') {
                    throw new IllegalArgumentException("Credential type segments must start with a lowercase letter");
                }
                segmentStart = false;
                continue;
            }
            if (character == '-') {
                if (previousHyphen) {
                    throw new IllegalArgumentException("Credential type name must not contain consecutive hyphens");
                }
                previousHyphen = true;
                continue;
            }
            if ((character < 'a' || character > 'z') && (character < '0' || character > '9')) {
                throw new IllegalArgumentException("Credential type name contains an unsupported character");
            }
            previousHyphen = false;
        }
        if (segmentStart || previousHyphen) {
            throw new IllegalArgumentException("Credential type name must not end with a separator or hyphen");
        }
    }
}
