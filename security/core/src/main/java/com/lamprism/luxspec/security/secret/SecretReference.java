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

package com.lamprism.luxspec.security.secret;

import com.lamprism.luxspec.validation.Validator;

import java.util.Objects;

/**
 * Opaque provider-owned reference to secret material.
 *
 * <p>The reference identifies secret material but never contains the material itself. Provider
 * adapters may interpret it as a path, URI, alias, or external secret-manager identifier.</p>
 *
 * @author RollW
 */
public final class SecretReference {
    private static final int MAXIMUM_LENGTH = 512;

    private final String value;

    private SecretReference(String value) {
        this.value = requireValue(value);
    }

    /**
     * Creates a provider-owned secret reference.
     *
     * @param value the non-blank reference
     * @return the immutable reference
     */
    public static SecretReference of(String value) {
        return new SecretReference(value);
    }

    /**
     * Returns the provider-owned reference text.
     *
     * @return the reference text
     */
    public String value() {
        return value;
    }

    /**
     * Returns the provider-owned reference text.
     *
     * @return the reference text
     */
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SecretReference reference)) {
            return false;
        }
        return value.equals(reference.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "SecretReference[value=" + value + "]";
    }

    private static String requireValue(String value) {
        String nonNullValue = Objects.requireNonNull(value, "value");
        Validator.nonBlank("Secret reference").validate(nonNullValue);
        Validator.noControlCharacters("Secret reference").validate(nonNullValue);
        if (nonNullValue.length() > MAXIMUM_LENGTH) {
            throw new IllegalArgumentException("Secret reference exceeds the maximum length");
        }
        return nonNullValue;
    }
}
