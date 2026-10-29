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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 *
 * @author RollW
 */
final class OrderedMultiLevelCache<K, V> implements MultiLevelCache<K, V> {
    private final List<Cache<K, V>> levels;
    private final Object stateLock = new Object();
    private long generation;

    OrderedMultiLevelCache(List<? extends Cache<K, V>> levels) {
        List<? extends Cache<K, V>> nonNullLevels = Objects.requireNonNull(levels, "levels");
        if (nonNullLevels.size() < 2) {
            throw new IllegalArgumentException("A multi-level cache requires at least two levels");
        }
        List<Cache<K, V>> copied = new ArrayList<>(nonNullLevels.size());
        for (Cache<K, V> level : nonNullLevels) {
            copied.add(Objects.requireNonNull(level, "level"));
        }
        this.levels = List.copyOf(copied);
    }

    @Override
    public @Nullable V getIfPresent(K key) {
        K nonNullKey = Objects.requireNonNull(key, "key");
        long lookupGeneration;
        synchronized (stateLock) {
            lookupGeneration = generation;
        }
        for (int index = 0; index < levels.size(); index++) {
            V value = levels.get(index).getIfPresent(nonNullKey);
            if (value != null) {
                synchronized (stateLock) {
                    if (lookupGeneration == generation) {
                        promoteToAllNearerLevels(nonNullKey, value, index);
                    }
                }
                return value;
            }
        }
        return null;
    }

    @Override
    public V get(K key, Function<? super K, ? extends V> loader) {
        K nonNullKey = Objects.requireNonNull(key, "key");
        Function<? super K, ? extends V> nonNullLoader = Objects.requireNonNull(loader, "loader");
        long loadGeneration;
        synchronized (stateLock) {
            loadGeneration = generation;
        }
        AtomicBoolean loaded = new AtomicBoolean();
        Cache<K, V> first = levels.get(0);
        V value = first.get(nonNullKey, ignored -> {
            loaded.set(true);
            return loadLowerLevelOrOrigin(nonNullKey, nonNullLoader);
        });
        if (loaded.get()) {
            synchronized (stateLock) {
                if (loadGeneration == generation) {
                    writeThroughLowerLevels(nonNullKey, value);
                }
            }
        }
        return value;
    }

    @Override
    public void put(K key, V value) {
        K nonNullKey = Objects.requireNonNull(key, "key");
        V nonNullValue = Objects.requireNonNull(value, "value");
        synchronized (stateLock) {
            generation++;
            for (int index = levels.size() - 1; index >= 0; index--) {
                levels.get(index).put(nonNullKey, nonNullValue);
            }
        }
    }

    @Override
    public void invalidate(K key) {
        K nonNullKey = Objects.requireNonNull(key, "key");
        synchronized (stateLock) {
            generation++;
            for (int index = levels.size() - 1; index >= 0; index--) {
                levels.get(index).invalidate(nonNullKey);
            }
        }
    }

    @Override
    public void invalidateAll() {
        synchronized (stateLock) {
            generation++;
            for (int index = levels.size() - 1; index >= 0; index--) {
                levels.get(index).invalidateAll();
            }
        }
    }

    @Override
    public void invalidateAll(Predicate<? super K> predicate) {
        Predicate<? super K> nonNullPredicate = Objects.requireNonNull(predicate, "predicate");
        synchronized (stateLock) {
            generation++;
            for (int index = levels.size() - 1; index >= 0; index--) {
                levels.get(index).invalidateAll(nonNullPredicate);
            }
        }
    }

    private V loadLowerLevelOrOrigin(K key, Function<? super K, ? extends V> loader) {
        for (int index = 1; index < levels.size(); index++) {
            V value = levels.get(index).getIfPresent(key);
            if (value != null) {
                return value;
            }
        }
        V loaded = Objects.requireNonNull(loader.apply(key), "loaded value");
        return loaded;
    }

    private void promoteToAllNearerLevels(K key, V value, int hitIndex) {
        for (int index = hitIndex - 1; index >= 0; index--) {
            levels.get(index).put(key, value);
        }
    }

    private void writeThroughLowerLevels(K key, V value) {
        for (int index = levels.size() - 1; index >= 1; index--) {
            levels.get(index).put(key, value);
        }
    }
}
