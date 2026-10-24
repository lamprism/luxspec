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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiLevelCacheTest {
    @Test
    void promotesALowerLevelHitWithoutCallingTheLoader() {
        MapCache<String, String> near = new MapCache<>();
        MapCache<String, String> far = new MapCache<>();
        far.put("key", "cached");
        MultiLevelCache<String, String> cache = MultiLevelCache.of(List.of(near, far));

        String value = cache.get("key", ignored -> {
            throw new AssertionError("Loader must not be called");
        });

        assertEquals("cached", value);
        assertEquals("cached", near.getIfPresent("key"));
    }

    @Test
    void promotesALowerLevelPresentLookup() {
        MapCache<String, String> near = new MapCache<>();
        MapCache<String, String> far = new MapCache<>();
        far.put("key", "cached");
        MultiLevelCache<String, String> cache = MultiLevelCache.of(List.of(near, far));

        assertEquals("cached", cache.getIfPresent("key"));

        assertEquals("cached", near.getIfPresent("key"));
    }

    @Test
    void writesLoadedAndExplicitValuesThroughEveryLevel() {
        MapCache<String, String> near = new MapCache<>();
        MapCache<String, String> far = new MapCache<>();
        MultiLevelCache<String, String> cache = MultiLevelCache.of(List.of(near, far));

        assertEquals("loaded", cache.get("loaded", ignored -> "loaded"));
        cache.put("explicit", "value");

        assertEquals("loaded", near.getIfPresent("loaded"));
        assertEquals("loaded", far.getIfPresent("loaded"));
        assertEquals("value", near.getIfPresent("explicit"));
        assertEquals("value", far.getIfPresent("explicit"));
    }

    @Test
    void doesNotRepublishAnOriginLoadAfterCompositeInvalidation() throws Exception {
        assertInvalidationDuringLoad(cache -> cache.invalidate("key"));
        assertInvalidationDuringLoad(MultiLevelCache::invalidateAll);
        assertInvalidationDuringLoad(cache -> cache.invalidateAll("key"::equals));
    }

    @Test
    void doesNotOverwriteExplicitWritesWithAnOriginLoadAlreadyInProgress() throws Exception {
        CaffeineCache<String, String> near = new CaffeineCache<>(
                com.github.benmanes.caffeine.cache.Caffeine.newBuilder().build()
        );
        MapCache<String, String> far = new MapCache<>();
        MultiLevelCache<String, String> cache = MultiLevelCache.of(List.of(near, far));
        CountDownLatch originEntered = new CountDownLatch(1);
        CountDownLatch releaseOrigin = new CountDownLatch(1);
        AtomicInteger loaderCalls = new AtomicInteger();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<String> loadedValue = executor.submit(() -> cache.get("key", ignored -> {
                loaderCalls.incrementAndGet();
                originEntered.countDown();
                await(releaseOrigin);
                return "stale";
            }));

            assertTrue(originEntered.await(5L, TimeUnit.SECONDS));
            cache.put("key", "explicit");
            releaseOrigin.countDown();

            assertEquals("explicit", loadedValue.get(5L, TimeUnit.SECONDS));
            assertEquals(1, loaderCalls.get());
            assertEquals("explicit", near.getIfPresent("key"));
            assertEquals("explicit", far.getIfPresent("key"));

            near.invalidate("key");
            assertEquals("explicit", cache.getIfPresent("key"));
            assertEquals("explicit", near.getIfPresent("key"));
        } finally {
            releaseOrigin.countDown();
            executor.shutdownNow();
        }
    }

    private static void assertInvalidationDuringLoad(
            Consumer<MultiLevelCache<String, String>> invalidation
    ) throws Exception {
        CaffeineCache<String, String> near = new CaffeineCache<>(
                com.github.benmanes.caffeine.cache.Caffeine.newBuilder().build()
        );
        MapCache<String, String> far = new MapCache<>();
        MultiLevelCache<String, String> cache = MultiLevelCache.of(List.of(near, far));
        CountDownLatch originEntered = new CountDownLatch(1);
        CountDownLatch releaseOrigin = new CountDownLatch(1);
        AtomicInteger loaderCalls = new AtomicInteger();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<String> loadedValue = executor.submit(() -> cache.get("key", ignored -> {
                int call = loaderCalls.incrementAndGet();
                if (call == 1) {
                    originEntered.countDown();
                    await(releaseOrigin);
                    return "stale";
                }
                return "fresh";
            }));

            assertTrue(originEntered.await(5L, TimeUnit.SECONDS));
            invalidation.accept(cache);
            releaseOrigin.countDown();

            assertEquals("fresh", loadedValue.get(5L, TimeUnit.SECONDS));
            assertEquals(2, loaderCalls.get());
            assertNull(far.getIfPresent("key"));
        } finally {
            releaseOrigin.countDown();
            executor.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Cache load was interrupted", failure);
        }
    }

    @Test
    void invalidatesEveryLevel() {
        MapCache<String, String> near = new MapCache<>();
        MapCache<String, String> far = new MapCache<>();
        MultiLevelCache<String, String> cache = MultiLevelCache.of(List.of(near, far));
        cache.put("first", "one");
        cache.put("second", "two");

        cache.invalidateAll(key -> key.equals("first"));

        assertNull(near.getIfPresent("first"));
        assertNull(far.getIfPresent("first"));
        assertEquals("two", near.getIfPresent("second"));
        assertEquals("two", far.getIfPresent("second"));
    }

    @Test
    void requiresAtLeastTwoLevels() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MultiLevelCache.of(List.of(new MapCache<String, String>()))
        );
    }

    @Test
    void createsSingleLevelCachesThroughThePlanContract() {
        CreatingCacheFactory factory = new CreatingCacheFactory();
        CacheName name = CacheName.of("test");
        CacheProfile profile = CacheProfile.builder().maximumSize(10).build();
        CachePlan plan = CachePlan.single(factory, profile);

        Cache<String, String> cache = plan.create(name);

        assertEquals(1, factory.creations.get());
        assertEquals(List.of(name), factory.names);
        assertEquals(List.of(profile), factory.profiles);
        assertFalse(cache instanceof MultiLevelCache);
    }

    @Test
    void createsMultiLevelCachesThroughTheSamePlanContractAsSingleLevelCaches() {
        CreatingCacheFactory factory = new CreatingCacheFactory();
        CacheName name = CacheName.of("test");
        CacheProfile nearProfile = CacheProfile.builder().maximumSize(10).build();
        CacheProfile farProfile = CacheProfile.builder().maximumSize(100).build();
        CachePlan plan = CachePlan.multi(List.of(
                CacheLevel.of(factory, nearProfile),
                CacheLevel.of(factory, farProfile)
        ));

        Cache<String, String> cache = plan.create(name);

        assertEquals(2, factory.creations.get());
        assertEquals(List.of(name, name), factory.names);
        assertEquals(List.of(nearProfile, farProfile), factory.profiles);
        assertTrue(cache instanceof MultiLevelCache);
    }

    @Test
    void multiLevelPlanRequiresAtLeastTwoConfiguredLevels() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CachePlan.multi(List.of(CacheLevel.of(
                        new CreatingCacheFactory(),
                        CacheProfile.defaults()
                )))
        );
    }

    private static final class CreatingCacheFactory implements CacheFactory {
        private final AtomicInteger creations = new AtomicInteger();
        private final List<CacheName> names = new ArrayList<>();
        private final List<CacheProfile> profiles = new ArrayList<>();

        @Override
        public <K, V> Cache<K, V> create(CacheName name, CacheProfile profile) {
            creations.incrementAndGet();
            names.add(name);
            profiles.add(profile);
            return new MapCache<>();
        }
    }

    private static final class MapCache<K, V> implements Cache<K, V> {
        private final Map<K, V> values = new HashMap<>();

        @Override
        public V getIfPresent(K key) {
            return values.get(key);
        }

        @Override
        public V get(K key, Function<? super K, ? extends V> loader) {
            return values.computeIfAbsent(key, loader);
        }

        @Override
        public void put(K key, V value) {
            values.put(key, value);
        }

        @Override
        public void invalidate(K key) {
            values.remove(key);
        }

        @Override
        public void invalidateAll() {
            values.clear();
        }

        @Override
        public void invalidateAll(Predicate<? super K> predicate) {
            values.keySet().removeIf(predicate);
        }
    }
}
