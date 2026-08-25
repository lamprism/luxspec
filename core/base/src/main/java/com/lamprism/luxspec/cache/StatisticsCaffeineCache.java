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

import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Caffeine cache variant that collects statistics only when explicitly selected.
 *
 * @param <K> the cache key type
 * @param <V> the cache value type
 * @author RollW
 */
class StatisticsCaffeineCache<K, V> implements Cache<K, V>, CacheStatisticsSource {
    private final Cache<K, V> cache;
    private final com.github.benmanes.caffeine.cache.Cache<K, V> delegate;
    private final LongAdder loadSuccessCount = new LongAdder();
    private final LongAdder loadFailureCount = new LongAdder();
    private final LongAdder loadDurationNanos = new LongAdder();

    StatisticsCaffeineCache(com.github.benmanes.caffeine.cache.Cache<K, V> delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.cache = new CaffeineCache<>(this.delegate);
    }

    @Override
    public V get(K key, Function<? super K, ? extends V> loader) {
        return cache.get(key, loadedKey -> load(loadedKey, loader));
    }

    @Override
    @Nullable
    public V getIfPresent(K key) {
        return cache.getIfPresent(key);
    }

    @Override
    public void put(K key, V value) {
        cache.put(key, value);
    }

    @Override
    public void invalidate(K key) {
        cache.invalidate(key);
    }

    @Override
    public void invalidateAll() {
        cache.invalidateAll();
    }

    @Override
    public void invalidateAll(Predicate<? super K> predicate) {
        cache.invalidateAll(predicate);
    }

    private V load(K key, Function<? super K, ? extends V> loader) {
        long startedAt = System.nanoTime();
        try {
            V value = loader.apply(key);
            if (value == null) {
                throw new NullPointerException("loaded value");
            }
            loadSuccessCount.increment();
            loadDurationNanos.add(System.nanoTime() - startedAt);
            return value;
        } catch (RuntimeException | Error failure) {
            loadFailureCount.increment();
            loadDurationNanos.add(System.nanoTime() - startedAt);
            throw failure;
        }
    }

    @Override
    public CacheStats stats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats statistics = delegate.stats();
        return new CacheStats(
                statistics.hitCount(),
                statistics.missCount(),
                loadSuccessCount.sum(),
                loadFailureCount.sum(),
                loadDurationNanos.sum(),
                statistics.evictionCount(),
                delegate.estimatedSize()
        );
    }
}
