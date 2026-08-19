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

package com.lamprism.luxspec.user.security.password;

import java.util.Objects;

/**
 * Holds one opaque encoded password representation suitable for protected storage.
 *
 * @author RollW
 */
public final class EncodedPassword {
    /**
     * The largest representation accepted by the initial local-user schema.
     */
    public static final int MAXIMUM_LENGTH = 255;

    private final String value;

    /**
     * Creates one bounded non-blank encoded password representation.
     *
     * @param value the opaque storage representation
     */
    public EncodedPassword(String value) {
        this.value = requireValue(value);
    }

    /**
     * Returns the exact opaque representation for a protected storage or adapter boundary.
     *
     * @return the encoded password representation
     */
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "EncodedPassword[redacted]";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof EncodedPassword password && value.equals(password.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    private static String requireValue(String candidate) {
        String nonNullValue = Objects.requireNonNull(candidate, "value");
        if (nonNullValue.isBlank()) {
            throw new IllegalArgumentException("Encoded password must not be blank");
        }
        if (nonNullValue.length() > MAXIMUM_LENGTH) {
            throw new IllegalArgumentException("Encoded password exceeds the storage limit");
        }
        return nonNullValue;
    }
}
