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

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaffeineCacheTest {
    @Test
    void coalescesConcurrentLoadsForOneKey() throws Exception {
        CacheProfile profile = CacheProfile.builder()
                .maximumSize(16)
                .expireAfterWrite(Duration.ofMinutes(1))
                .build();
        Cache<String, Integer> cache = new CaffeineCacheFactory()
                .create(CacheName.of("test"), profile);
        AtomicInteger loads = new AtomicInteger();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<Integer> first = new AtomicReference<>();
        AtomicReference<Integer> second = new AtomicReference<>();

        Thread firstThread = new Thread(() -> first.set(cache.get("key", key -> load(
                loads,
                started,
                release,
                42
        ))));
        Thread secondThread = new Thread(() -> second.set(cache.get("key", key -> load(
                loads,
                started,
                release,
                42
        ))));
        firstThread.start();
        assertTrue(started.await(5, TimeUnit.SECONDS));
        secondThread.start();
        release.countDown();
        firstThread.join(5_000);
        secondThread.join(5_000);

        assertEquals(1, loads.get());
        assertEquals(42, first.get());
        assertEquals(42, second.get());
    }

    @Test
    void invalidationPreventsAnInFlightLoadFromRemainingCached() throws Exception {
        Cache<String, String> cache = new CaffeineCacheFactory()
                .create(CacheName.of("test"), CacheProfile.defaults());
        AtomicInteger loads = new AtomicInteger();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();

        Thread reader = new Thread(() -> result.set(cache.get("key", key -> {
            if (loads.incrementAndGet() == 1) {
                started.countDown();
                await(release);
                return "stale";
            }
            return "fresh";
        })));
        reader.start();
        assertTrue(started.await(5, TimeUnit.SECONDS));
        cache.invalidate("key");
        release.countDown();
        reader.join(5_000);

        assertEquals("fresh", result.get());
        assertEquals("fresh", cache.getIfPresent("key"));
        assertEquals(2, loads.get());
        cache.invalidateAll();
        assertNull(cache.getIfPresent("key"));
    }

    @Test
    void explicitPutPreventsAnInFlightLoadFromOverwritingTheValue() throws Exception {
        Cache<String, String> cache = new CaffeineCacheFactory()
                .create(CacheName.of("test"), CacheProfile.defaults());
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();

        Thread reader = new Thread(() -> result.set(cache.get("key", key -> {
            started.countDown();
            await(release);
            return "stale";
        })));
        reader.start();
        assertTrue(started.await(5, TimeUnit.SECONDS));
        cache.put("key", "explicit");
        release.countDown();
        reader.join(5_000);

        assertEquals("explicit", result.get());
        assertEquals("explicit", cache.getIfPresent("key"));
    }

    @Test
    void collectsStatisticsOnlyWhenExplicitlyEnabled() {
        Cache<String, String> uninstrumented = new CaffeineCacheFactory()
                .create(CacheName.of("plain"), CacheProfile.defaults());
        uninstrumented.get("key", key -> "value");
        assertFalse(uninstrumented instanceof CacheStatisticsSource);

        CacheProfile profile = CacheProfile.builder().recordStats(true).build();
        Cache<String, String> instrumented = new CaffeineCacheFactory()
                .create(CacheName.of("observed"), profile);
        instrumented.get("key", key -> "value");
        instrumented.getIfPresent("missing");
        assertTrue(instrumented instanceof CacheStatisticsSource);
        CacheStats stats = ((CacheStatisticsSource) instrumented).stats();
        assertEquals(1L, stats.getLoadSuccessCount());
        assertEquals(2L, stats.getMissCount());
        assertTrue(stats.getLoadDurationNanos() >= 0L);
    }

    @Test
    void appliesProfilesPerCreatedCache() {
        CacheName name = CacheName.of("shared-name");
        CacheProfile observedProfile = CacheProfile.builder().recordStats(true).build();
        CaffeineCacheFactory factory = new CaffeineCacheFactory();

        Cache<String, String> plain = factory.create(name, CacheProfile.defaults());
        Cache<String, String> observed = factory.create(name, observedProfile);

        assertFalse(plain instanceof CacheStatisticsSource);
        assertTrue(observed instanceof CacheStatisticsSource);
    }

    private static Integer load(
            AtomicInteger loads,
            CountDownLatch started,
            CountDownLatch release,
            Integer value
    ) {
        loads.incrementAndGet();
        started.countDown();
        await(release);
        return value;
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for cache test coordination");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Cache test coordination was interrupted", exception);
        }
    }
}
