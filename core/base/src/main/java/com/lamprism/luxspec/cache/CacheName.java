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
 * Identifies one cache ownership boundary independently of a cache provider.
 *
 * <p>The stable name may be used during cache assembly and as a bounded monitoring dimension. It
 * is an opaque, non-blank application identifier. Provider adapters are responsible for
 * translating it to provider-specific namespaces or keys.</p>
 *
 * @author RollW
 */
public final class CacheName {
    private final String value;

    private CacheName(String value) {
        this.value = value;
    }

    /**
     * Creates a cache name from a non-blank identifier.
     *
     * @param value the stable cache name
     * @return the cache name
     */
    public static CacheName of(String value) {
        String nonNullValue = Objects.requireNonNull(value, "value");
        if (nonNullValue.isBlank()) {
            throw new IllegalArgumentException("Cache name must not be blank");
        }
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
}
