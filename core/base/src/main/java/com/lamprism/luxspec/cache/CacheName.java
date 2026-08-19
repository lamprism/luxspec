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

package com.lamprism.luxspec.cache;

import java.util.Objects;

/**
 * Identifies one cache ownership boundary.
 *
 * @author RollW
 */
public final class CacheName {
    private final String value;

    private CacheName(String value) {
        this.value = value;
    }

    /**
     * Creates a validated cache name.
     *
     * @param value the stable cache name
     * @return the cache name
     */
    public static CacheName of(String value) {
        String nonNullValue = Objects.requireNonNull(value, "value");
        validate(nonNullValue);
        return new CacheName(nonNullValue);
    }

    /**
     * Returns the stable cache name.
     *
     * @return the cache name
     */
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CacheName cacheName && value.equals(cacheName.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }

    private static void validate(String value) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Cache name must not be empty");
        }
        boolean segmentStart = true;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '.') {
                if (segmentStart) {
                    throw new IllegalArgumentException("Cache name contains an empty segment");
                }
                segmentStart = true;
                continue;
            }
            if (!isAllowed(character)) {
                throw new IllegalArgumentException("Cache name contains an unsupported character");
            }
            segmentStart = false;
        }
        if (segmentStart) {
            throw new IllegalArgumentException("Cache name must not end with a separator");
        }
    }

    private static boolean isAllowed(char character) {
        return character >= 'a' && character <= 'z'
                || character >= 'A' && character <= 'Z'
                || character >= '0' && character <= '9'
                || character == '-'
                || character == '_';
    }
}
