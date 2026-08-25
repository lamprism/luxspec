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

package com.lamprism.luxspec.observability.runtime;

import com.lamprism.luxspec.context.ExecutionContextStorage;
import com.lamprism.luxspec.context.ThreadLocalExecutionContextStorage;
import com.lamprism.luxspec.observability.ObservabilityClock;
import com.lamprism.luxspec.observability.health.HealthGroup;
import com.lamprism.luxspec.observability.health.HealthRegistry;
import com.lamprism.luxspec.observability.health.HealthResult;
import com.lamprism.luxspec.observability.health.HealthStatus;
import com.lamprism.luxspec.observability.metric.CardinalityOverflow;
import com.lamprism.luxspec.observability.metric.Counter;
import com.lamprism.luxspec.observability.metric.CounterSpec;
import com.lamprism.luxspec.observability.metric.MetricBinding;
import com.lamprism.luxspec.observability.metric.MetricCardinalityException;
import com.lamprism.luxspec.observability.metric.MetricCardinalityPolicy;
import com.lamprism.luxspec.observability.metric.MetricDimensionSet;
import com.lamprism.luxspec.observability.metric.MetricDimensionSpec;
import com.lamprism.luxspec.observability.metric.MetricRegistry;
import com.lamprism.luxspec.observability.metric.MetricSnapshot;
import com.lamprism.luxspec.observability.metric.Timer;
import com.lamprism.luxspec.observability.metric.TimerSpec;
import com.lamprism.luxspec.observability.observation.Observation;
import com.lamprism.luxspec.observability.observation.ObservationAttributeClassification;
import com.lamprism.luxspec.observability.observation.ObservationAttributeSet;
import com.lamprism.luxspec.observability.observation.ObservationAttributeSpec;
import com.lamprism.luxspec.observability.observation.ObservationHandler;
import com.lamprism.luxspec.observability.observation.ObservationOutcome;
import com.lamprism.luxspec.observability.observation.ObservationSpec;
import com.lamprism.luxspec.observability.observation.ObservationStart;
import com.lamprism.luxspec.observability.observation.ObservationView;
import com.lamprism.luxspec.observability.runtime.health.HealthRegistryBuilder;
import com.lamprism.luxspec.observability.runtime.metric.MetricRegistryBuilder;
import com.lamprism.luxspec.observability.runtime.observation.ObservationRegistryBuilder;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservabilityRuntimeTest {
    @Test
    void materializesDimensionedCountersAndCapturesOneSnapshot() {
        MetricDimensionSpec<String> route = MetricDimensionSpec.required("route");
        CounterSpec spec = CounterSpec.builder("http.requests")
                .dimension(route)
                .build();

        try (MetricRegistry registry = MetricRegistryBuilder.builder().build()) {
            registry.register(spec);
            MetricBinding<Counter> firstBinding = spec.bind(MetricDimensionSet.of(route, "/accounts"));
            Counter first = registry.obtain(firstBinding);
            Counter same = registry.obtain(firstBinding);
            Counter second = registry.obtain(spec.bind(MetricDimensionSet.of(route, "/health")));

            first.increment();
            second.add(2.0d);

            assertSame(first, same);
            MetricSnapshot snapshot = registry.snapshot();
            assertEquals(2, snapshot.readings().size());
            assertEquals(1.0d, snapshot.readings().get(0).value());
            assertEquals(2.0d, snapshot.readings().get(1).value());
        }
    }

    @Test
    void appliesCardinalityPolicyAndTimerClock() {
        FakeClock clock = new FakeClock();
        MetricDimensionSpec<String> route = MetricDimensionSpec.required("route");
        CounterSpec bounded = CounterSpec.builder("requests")
                .dimension(route)
                .cardinality(MetricCardinalityPolicy.bounded(1, CardinalityOverflow.REJECT))
                .build();
        TimerSpec timerSpec = TimerSpec.builder("latency").build();

        try (MetricRegistry registry = MetricRegistryBuilder.builder().clock(clock).build()) {
            registry.register(bounded);
            registry.register(timerSpec);
            registry.obtain(bounded.bind(MetricDimensionSet.of(route, "one")));
            assertThrows(
                    MetricCardinalityException.class,
                    () -> registry.obtain(bounded.bind(MetricDimensionSet.of(route, "two")))
            );

            Timer timer = registry.obtain(timerSpec);
            try (var timing = timer.start()) {
                clock.advance(Duration.ofMillis(25));
            }
            assertEquals(Duration.ofMillis(25), timer.totalTime());
            assertEquals(1L, timer.count());
        }
    }

    @Test
    void runsObservationLifecycleWithExplicitParent() {
        ObservationSpec parentSpec = ObservationSpec.builder("request").build();
        ObservationSpec childSpec = ObservationSpec.builder("database").build();
        ObservationAttributeSpec<String> route = ObservationAttributeSpec.string(
                "route",
                ObservationAttributeClassification.LOW
        );
        List<String> callbacks = new ArrayList<>();
        List<ObservationView> starts = new ArrayList<>();
        ObservationHandler handler = new ObservationHandler() {
            @Override
            public void onStart(ObservationView view) {
                starts.add(view);
                callbacks.add("start:" + view.spec().getName().value());
            }

            @Override
            public void onError(ObservationView view) {
                callbacks.add("error");
            }

            @Override
            public void onStop(ObservationView view) {
                callbacks.add("stop:" + view.spec().getName().value());
            }
        };

        try (var registry = ObservationRegistryBuilder.builder().handler(handler).build()) {
            registry.register(parentSpec);
            registry.register(childSpec);
            Observation parent = registry.start(ObservationStart.builder(parentSpec)
                    .attributes(ObservationAttributeSet.builder().put(route, "/accounts").build())
                    .build());
            Observation child = registry.start(ObservationStart.builder(childSpec)
                    .parent(parent)
                    .build());
            child.error(new IllegalStateException("expected"));
            child.stop();
            parent.setOutcome(ObservationOutcome.SUCCESS);
            parent.stop();

            assertEquals(2, starts.size());
            ObservationView childView = starts.get(1);
            assertEquals(childSpec, childView.spec());
            assertEquals(starts.get(0).id(), childView.parentId());
            assertTrue(callbacks.contains("error"));
            assertEquals(ObservationOutcome.ERROR, child.outcome());
            assertEquals(ObservationOutcome.SUCCESS, parent.outcome());
        }
    }

    @Test
    void usesTheSelectedContextStorageForObservationScopes() {
        ObservationSpec parentSpec = ObservationSpec.builder("request").build();
        ObservationSpec childSpec = ObservationSpec.builder("database").build();
        ExecutionContextStorage storage = new ThreadLocalExecutionContextStorage();
        List<String> callbacks = new ArrayList<>();
        List<ObservationView> starts = new ArrayList<>();
        ObservationHandler handler = new ObservationHandler() {
            @Override
            public void onStart(ObservationView view) {
                starts.add(view);
            }

            @Override
            public void onScopeOpened(ObservationView view) {
                callbacks.add("open");
            }

            @Override
            public void onScopeClosed(ObservationView view) {
                callbacks.add("close");
            }
        };

        try (var registry = ObservationRegistryBuilder.builder()
                .contextStorage(storage)
                .handler(handler)
                .build()) {
            registry.register(parentSpec);
            registry.register(childSpec);
            Observation parent = registry.start(parentSpec);
            try (var ignored = parent.openScope()) {
                Observation child = registry.start(childSpec);
                assertEquals(starts.get(0).id(), starts.get(1).parentId());
                child.stop();
            }
            parent.stop();
        }

        assertEquals(List.of("open", "close"), callbacks);
        assertTrue(storage.current().isEmpty());
    }

    @Test
    void isolatesPredicateAndFilterFailures() {
        ObservationSpec spec = ObservationSpec.builder("request").build();
        List<String> callbacks = new ArrayList<>();
        ObservationHandler handler = new ObservationHandler() {
            @Override
            public void onStart(ObservationView view) {
                callbacks.add("start");
            }

            @Override
            public void onStop(ObservationView view) {
                callbacks.add("stop");
            }
        };

        try (var registry = ObservationRegistryBuilder.builder()
                .predicate(start -> {
                    throw new IllegalStateException("predicate failure");
                })
                .handler(handler)
                .build()) {
            registry.register(spec);
            registry.start(spec).stop();
            assertTrue(callbacks.isEmpty());
        }

        try (var registry = ObservationRegistryBuilder.builder()
                .filter(start -> {
                    throw new IllegalStateException("filter failure");
                })
                .handler(handler)
                .build()) {
            registry.register(spec);
            registry.start(spec).stop();
            assertEquals(List.of("start", "stop"), callbacks);
        }
    }

    @Test
    void aggregatesHealthContributorsAndIsolatesFailures() {
        Executor executor = Runnable::run;
        HealthRegistry registry = HealthRegistryBuilder.builder(executor)
                .register("database", () -> HealthResult.of(HealthStatus.UP))
                .register("queue", () -> {
                    throw new IllegalStateException("unavailable");
                })
                .include(HealthGroup.READINESS, "database")
                .build();

        assertEquals(HealthStatus.DOWN, registry.evaluate(HealthGroup.AGGREGATE).getStatus());
        assertEquals(HealthStatus.UP, registry.evaluate(HealthGroup.READINESS).getStatus());
        assertEquals(HealthStatus.UP, registry.evaluate(HealthGroup.LIVENESS).getStatus());
    }

    @Test
    void returnsHealthTimeoutWithoutWaitingForContributor() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch release = new CountDownLatch(1);
        try {
            HealthRegistry registry = HealthRegistryBuilder.builder(executor)
                    .timeout(Duration.ofMillis(10))
                    .register("slow", () -> {
                        try {
                            release.await();
                        } catch (InterruptedException failure) {
                            Thread.currentThread().interrupt();
                        }
                        return HealthResult.of(HealthStatus.UP);
                    })
                    .build();

            assertEquals(HealthStatus.DOWN, registry.evaluate(HealthGroup.AGGREGATE).getStatus());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    private static final class FakeClock implements ObservabilityClock {
        private long nanos;
        private Instant instant = Instant.parse("2026-01-01T00:00:00Z");

        @Override
        public long monotonicNanos() {
            return nanos;
        }

        @Override
        public Instant now() {
            return instant;
        }

        private void advance(Duration duration) {
            nanos += duration.toNanos();
            instant = instant.plus(duration);
        }
    }
}
