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

package com.lamprism.luxspec.observability.micrometer;

import com.lamprism.luxspec.observability.observation.ObservationLink;
import com.lamprism.luxspec.observability.observation.ObservationSpec;
import com.lamprism.luxspec.observability.observation.ObservationStart;
import com.lamprism.luxspec.observability.runtime.observation.ObservationRegistryBuilder;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author RollW
 */
class MicrometerObservationBridgeTest {
    @Test
    void preservesProviderNeutralLinksInTheProviderContext() {
        ObservationLink link = ObservationLink.of("trace-1", "span-1");
        List<ObservationLink> observedLinks = new ArrayList<>();
        io.micrometer.observation.ObservationRegistry providerRegistry =
                io.micrometer.observation.ObservationRegistry.create();
        providerRegistry.observationConfig().observationHandler(new ObservationHandler<Observation.Context>() {
            @Override
            public void onStart(Observation.Context context) {
                observedLinks.addAll(MicrometerObservationLinkContext.get(context));
            }

            @Override
            public boolean supportsContext(Observation.Context context) {
                return true;
            }
        });
        ObservationSpec spec = ObservationSpec.builder("test.operation").build();

        try (var registry = ObservationRegistryBuilder.builder().build();
             MicrometerObservationBridge ignored = new MicrometerObservationBridge(registry, providerRegistry)) {
            registry.register(spec);
            registry.start(ObservationStart.builder(spec).link(link).build()).stop();
        }

        assertEquals(List.of(link), observedLinks);
    }

    @Test
    void keepsConcurrentObservationScopesOnTheirOpeningThreads() throws Exception {
        io.micrometer.observation.ObservationRegistry providerRegistry =
                io.micrometer.observation.ObservationRegistry.create();
        ObservationSpec spec = ObservationSpec.builder("test.concurrent").build();
        try (var registry = ObservationRegistryBuilder.builder().build();
             MicrometerObservationBridge ignored = new MicrometerObservationBridge(registry, providerRegistry)) {
            registry.register(spec);
            com.lamprism.luxspec.observability.observation.Observation observation = registry.start(spec);
            CountDownLatch firstOpened = new CountDownLatch(1);
            CountDownLatch secondOpened = new CountDownLatch(1);
            CountDownLatch firstClosed = new CountDownLatch(1);
            AtomicReference<io.micrometer.observation.Observation> providerObservation = new AtomicReference<>();
            AtomicReference<Throwable> failure = new AtomicReference<>();
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                var first = executor.submit(() -> {
                    try (var scope = observation.openScope()) {
                        providerObservation.set(providerRegistry.getCurrentObservation());
                        firstOpened.countDown();
                        await(secondOpened);
                    } catch (Throwable caught) {
                        failure.compareAndSet(null, caught);
                    } finally {
                        firstClosed.countDown();
                    }
                });
                var second = executor.submit(() -> {
                    try {
                        await(firstOpened);
                        try (var scope = observation.openScope()) {
                            assertSame(providerObservation.get(), providerRegistry.getCurrentObservation());
                            secondOpened.countDown();
                            await(firstClosed);
                            assertSame(providerObservation.get(), providerRegistry.getCurrentObservation());
                        }
                        assertNull(providerRegistry.getCurrentObservation());
                    } catch (Throwable caught) {
                        failure.compareAndSet(null, caught);
                    }
                });

                first.get(5L, TimeUnit.SECONDS);
                second.get(5L, TimeUnit.SECONDS);
                assertNull(failure.get());
                observation.stop();
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Test
    void closesProviderScopeOnItsOwnerThreadAfterBridgeClose() throws Exception {
        io.micrometer.observation.ObservationRegistry providerRegistry =
                io.micrometer.observation.ObservationRegistry.create();
        ObservationSpec spec = ObservationSpec.builder("test.close-with-active-scope").build();
        try (var registry = ObservationRegistryBuilder.builder().build()) {
            registry.register(spec);
            MicrometerObservationBridge bridge = new MicrometerObservationBridge(registry, providerRegistry);
            var observation = registry.start(spec);
            CountDownLatch scopeOpened = new CountDownLatch(1);
            CountDownLatch allowScopeClose = new CountDownLatch(1);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                var providerScopeAfterClose = executor.submit(() -> {
                    try (var scope = observation.openScope()) {
                        Observation projected = providerRegistry.getCurrentObservation();
                        assertNotNull(projected);
                        scopeOpened.countDown();
                        await(allowScopeClose);
                        try (var nestedScope = observation.openScope()) {
                            assertSame(projected, providerRegistry.getCurrentObservation());
                        }
                        assertSame(projected, providerRegistry.getCurrentObservation());
                    }
                    return providerRegistry.getCurrentObservation();
                });

                assertTrue(scopeOpened.await(5L, TimeUnit.SECONDS));
                bridge.close();
                allowScopeClose.countDown();

                assertNull(providerScopeAfterClose.get(5L, TimeUnit.SECONDS));
            } finally {
                allowScopeClose.countDown();
                bridge.close();
                executor.shutdownNow();
            }
        }
    }

    @Test
    void stopsAnObservationWhoseStartRacesWithBridgeClose() throws Exception {
        CountDownLatch providerStartEntered = new CountDownLatch(1);
        CountDownLatch releaseProviderStart = new CountDownLatch(1);
        AtomicInteger stopCount = new AtomicInteger();
        io.micrometer.observation.ObservationRegistry providerRegistry =
                io.micrometer.observation.ObservationRegistry.create();
        providerRegistry.observationConfig().observationHandler(new ObservationHandler<Observation.Context>() {
            @Override
            public void onStart(Observation.Context context) {
                providerStartEntered.countDown();
                await(releaseProviderStart);
            }

            @Override
            public void onStop(Observation.Context context) {
                stopCount.incrementAndGet();
            }

            @Override
            public boolean supportsContext(Observation.Context context) {
                return true;
            }
        });
        ObservationSpec spec = ObservationSpec.builder("test.racing-start").build();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (var registry = ObservationRegistryBuilder.builder().build()) {
            registry.register(spec);
            MicrometerObservationBridge bridge = new MicrometerObservationBridge(registry, providerRegistry);
            try {
                var started = executor.submit(() -> registry.start(spec));
                assertTrue(providerStartEntered.await(5L, TimeUnit.SECONDS));
                bridge.close();
                releaseProviderStart.countDown();
                var observation = started.get(5L, TimeUnit.SECONDS);
                observation.stop();
                assertEquals(1, stopCount.get());
            } finally {
                releaseProviderStart.countDown();
                bridge.close();
                executor.shutdownNow();
            }
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5L, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for an observation lifecycle event");
            }
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Observation lifecycle wait was interrupted", failure);
        }
    }
}
