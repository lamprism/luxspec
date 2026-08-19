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

import org.jspecify.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Provides synchronous, successful-value caching without exposing a mutable map.
 *
 * <p>Implementations coalesce concurrent loads for the same key and do not retain loader failures.
 * Cache values and loader results must be non-null.</p>
 *
 * @param <K> the cache key type
 * @param <V> the cache value type
 * @author RollW
 */
public interface Cache<K, V> extends CacheInvalidator<K> {
    /**
     * Returns a cached value when it is currently present.
     *
     * @param key the non-null cache key
     * @return the cached value, or {@code null} on a miss
     */
    @Nullable V getIfPresent(K key);

    /**
     * Returns a cached value or atomically loads and stores one value for the key.
     *
     * @param key    the non-null cache key
     * @param loader the non-null successful-value loader
     * @return the cached or loaded value
     */
    V get(K key, Function<? super K, ? extends V> loader);

    /**
     * Stores one non-null value under a key.
     *
     * @param key   the non-null cache key
     * @param value the non-null cache value
     */
    void put(K key, V value);

    /**
     * Invalidates every cached entry.
     */
    void invalidateAll();

    /**
     * Invalidates every cached entry accepted by a predicate.
     *
     * @param predicate the non-null entry selector
     */
    void invalidateAll(Predicate<? super K> predicate);

}
