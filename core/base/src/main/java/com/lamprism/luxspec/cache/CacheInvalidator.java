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
 * Invalidates one entry in a cache owned by another component.
 *
 * @param <K> the cache key type
 * @author RollW
 */
@FunctionalInterface
public interface CacheInvalidator<K> {
    /**
     * Invalidates the entry identified by the key.
     *
     * @param key the non-null cache key
     */
    void invalidate(K key);

    /**
     * Returns a no-op invalidator for an optional cache integration.
     *
     * @param <K> the cache key type
     * @return the no-op invalidator
     */
    static <K> CacheInvalidator<K> noOp() {
        return key -> Objects.requireNonNull(key, "key");
    }
}
