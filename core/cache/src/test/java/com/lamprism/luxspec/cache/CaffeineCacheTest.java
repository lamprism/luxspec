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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaffeineCacheTest {
    @Test
    void coalescesConcurrentLoadsForOneKey() throws Exception {
        Cache<String, Integer> cache = CaffeineCaches.create(CacheProfile.builder()
                .maximumSize(16)
                .expireAfterWrite(Duration.ofMinutes(1))
                .build());
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
        Cache<String, String> cache = CaffeineCaches.create();
        AtomicInteger loads = new AtomicInteger();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch invalidationStarted = new CountDownLatch(1);
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
        Thread invalidator = new Thread(() -> {
            invalidationStarted.countDown();
            cache.invalidate("key");
        });
        invalidator.start();
        assertTrue(invalidationStarted.await(5, TimeUnit.SECONDS));
        release.countDown();
        reader.join(5_000);
        invalidator.join(5_000);

        assertEquals("fresh", result.get());
        assertEquals("fresh", cache.getIfPresent("key"));
        assertEquals(2, loads.get());
        cache.invalidateAll();
        assertNull(cache.getIfPresent("key"));
    }

    @Test
    void explicitPutPreventsAnInFlightLoadFromOverwritingTheValue() throws Exception {
        Cache<String, String> cache = CaffeineCaches.create();
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
