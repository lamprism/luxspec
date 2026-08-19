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
 * Creates bounded Caffeine-backed cache instances.
 *
 * @author RollW
 */
public class CaffeineCacheFactory implements CacheFactory {
    /**
     * Creates one factory.
     */
    public CaffeineCacheFactory() {
    }

    @Override
    public <K, V> Cache<K, V> create(CacheName name, CacheProfile profile) {
        Objects.requireNonNull(name, "name");
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
        if (nonNullProfile.isRecordStats()) {
            builder.recordStats();
        }
        com.github.benmanes.caffeine.cache.Cache<K, V> delegate = builder.build();
        if (nonNullProfile.isRecordStats()) {
            return new StatisticsCaffeineCache<>(delegate);
        }
        return new CaffeineCache<>(delegate);
    }
}
