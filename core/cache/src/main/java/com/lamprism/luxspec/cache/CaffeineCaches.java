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

import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.Objects;

/**
 * Creates the default Caffeine-backed implementation of the generic Cache contract.
 *
 * @author RollW
 */
public final class CaffeineCaches {
    private CaffeineCaches() {
    }

    /**
     * Creates a cache with the default bounded profile.
     *
     * @param <K> the cache key type
     * @param <V> the cache value type
     * @return the Caffeine-backed cache
     */
    public static <K, V> Cache<K, V> create() {
        return create(CacheProfile.defaults());
    }

    /**
     * Creates a cache from generic controls.
     *
     * @param profile the generic cache profile
     * @param <K>     the cache key type
     * @param <V>     the cache value type
     * @return the Caffeine-backed cache
     */
    public static <K, V> Cache<K, V> create(CacheProfile profile) {
        CacheProfile nonNullProfile = Objects.requireNonNull(profile, "profile");
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumSize(nonNullProfile.getMaximumSize());
        Duration expireAfterWrite = nonNullProfile.getExpireAfterWrite();
        if (expireAfterWrite != null) {
            builder.expireAfterWrite(expireAfterWrite);
        }
        Duration expireAfterAccess = nonNullProfile.getExpireAfterAccess();
        if (expireAfterAccess != null) {
            builder.expireAfterAccess(expireAfterAccess);
        }
        return new CaffeineCache<>(builder.build());
    }
}
