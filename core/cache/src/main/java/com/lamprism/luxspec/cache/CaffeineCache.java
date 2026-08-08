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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

final class CaffeineCache<K, V> implements com.lamprism.luxspec.cache.Cache<K, V> {
    private final com.github.benmanes.caffeine.cache.Cache<K, V> delegate;
    private final Map<K, LoadState<V>> inFlightLoads = new HashMap<>();
    private final Object stateLock = new Object();
    private long generation;

    CaffeineCache(com.github.benmanes.caffeine.cache.Cache<K, V> delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public @Nullable V getIfPresent(K key) {
        return delegate.getIfPresent(Cache.requireKey(key));
    }

    @Override
    public V get(K key, Function<? super K, ? extends V> loader) {
        K nonNullKey = Cache.requireKey(key);
        Function<? super K, ? extends V> nonNullLoader = Objects.requireNonNull(loader, "loader");
        while (true) {
            LoadState<V> loadState;
            boolean owner;
            synchronized (stateLock) {
                V cachedValue = delegate.getIfPresent(nonNullKey);
                if (cachedValue != null) {
                    return cachedValue;
                }
                loadState = inFlightLoads.get(nonNullKey);
                owner = loadState == null;
                if (owner) {
                    loadState = new LoadState<>(generation);
                    inFlightLoads.put(nonNullKey, loadState);
                }
            }
            if (!owner) {
                try {
                    return await(loadState);
                } catch (StaleLoadException exception) {
                    continue;
                }
            }

            V loadedValue;
            try {
                loadedValue = Cache.requireValue(nonNullLoader.apply(nonNullKey));
            } catch (Throwable failure) {
                completeFailure(nonNullKey, loadState, failure);
                return throwFailure(failure);
            }
            if (completeSuccess(nonNullKey, loadState, loadedValue)) {
                return loadedValue;
            }
        }
    }

    @Override
    public void put(K key, V value) {
        K nonNullKey = Cache.requireKey(key);
        V nonNullValue = Cache.requireValue(value);
        synchronized (stateLock) {
            LoadState<V> loadState = inFlightLoads.get(nonNullKey);
            if (loadState != null) {
                loadState.invalidated = true;
            }
            delegate.put(nonNullKey, nonNullValue);
        }
    }

    @Override
    public void invalidate(K key) {
        K nonNullKey = Cache.requireKey(key);
        synchronized (stateLock) {
            LoadState<V> loadState = inFlightLoads.get(nonNullKey);
            if (loadState != null) {
                loadState.invalidated = true;
            }
            delegate.invalidate(nonNullKey);
        }
    }

    @Override
    public void invalidateAll() {
        synchronized (stateLock) {
            generation++;
            delegate.invalidateAll();
        }
    }

    private void completeFailure(K key, LoadState<V> loadState, Throwable failure) {
        synchronized (stateLock) {
            loadState.future.completeExceptionally(failure);
            inFlightLoads.remove(key, loadState);
        }
    }

    private boolean completeSuccess(K key, LoadState<V> loadState, V value) {
        synchronized (stateLock) {
            boolean current = !loadState.invalidated && loadState.generation == generation;
            if (current) {
                delegate.put(key, value);
                loadState.future.complete(value);
            } else {
                loadState.future.completeExceptionally(new StaleLoadException());
            }
            inFlightLoads.remove(key, loadState);
            return current;
        }
    }

    private V await(LoadState<V> loadState) {
        try {
            return loadState.future.join();
        } catch (CompletionException exception) {
            return throwFailure(exception.getCause());
        }
    }

    private static <T> T throwFailure(Throwable failure) {
        if (failure instanceof RuntimeException exception) {
            throw exception;
        }
        if (failure instanceof Error error) {
            throw error;
        }
        throw new IllegalStateException("Cache loader failed", failure);
    }

    private static final class LoadState<V> {
        private final long generation;
        private final CompletableFuture<V> future = new CompletableFuture<>();
        private boolean invalidated;

        private LoadState(long generation) {
            this.generation = generation;
        }
    }

    private static final class StaleLoadException extends RuntimeException {
        private StaleLoadException() {
        }
    }
}
