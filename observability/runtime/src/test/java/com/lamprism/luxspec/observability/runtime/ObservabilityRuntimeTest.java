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

import com.lamprism.luxspec.context.ExecutionContext;
import com.lamprism.luxspec.context.ExecutionContextStorage;
import com.lamprism.luxspec.context.ThreadLocalExecutionContextStorage;
import com.lamprism.luxspec.observability.ObservabilityClock;
import com.lamprism.luxspec.observability.health.HealthAggregationPolicy;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author RollW
 */
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
    void restoresThePreviousContextIfOpeningAScopeCallbackThrowsAnError() {
        ExecutionContextStorage storage = new ThreadLocalExecutionContextStorage();
        ExecutionContext outer = ExecutionContext.empty();
        AssertionError callbackFailure = new AssertionError("scope callback failure");
        ObservationHandler handler = new ObservationHandler() {
            @Override
            public void onScopeOpened(ObservationView view) {
                throw callbackFailure;
            }
        };
        ObservationSpec spec = ObservationSpec.builder("scope").build();
        try (var registry = ObservationRegistryBuilder.builder().contextStorage(storage).handler(handler).build();
             ExecutionContextStorage.Scope ignored = storage.open(outer)) {
            registry.register(spec);
            Observation observation = registry.start(spec);

            assertSame(callbackFailure, assertThrows(AssertionError.class, observation::openScope));
            assertSame(outer, storage.current().orElseThrow());
            observation.stop();
        }
        assertTrue(storage.current().isEmpty());
    }

    @Test
    void replacesObservationAttributeSpecificationsAndTheirFormatters() {
        ObservationAttributeSpec<String> plain = ObservationAttributeSpec.of(
                "credential",
                String.class,
                ObservationAttributeClassification.LOW,
                value -> value
        );
        ObservationAttributeSpec<String> redacted = ObservationAttributeSpec.of(
                "credential",
                String.class,
                ObservationAttributeClassification.LOW,
                value -> "[REDACTED]"
        );
        ObservationSpec spec = ObservationSpec.builder("request").build();
        List<ObservationView> stopped = new ArrayList<>();
        ObservationHandler handler = new ObservationHandler() {
            @Override
            public void onStop(ObservationView view) {
                stopped.add(view);
            }
        };

        try (var registry = ObservationRegistryBuilder.builder().handler(handler).build()) {
            registry.register(spec);
            Observation observation = registry.start(ObservationStart.builder(spec)
                    .attributes(ObservationAttributeSet.builder().put(plain, "secret-value").build())
                    .build());
            observation.put(redacted, "secret-value");
            observation.stop();
        }

        ObservationView view = stopped.get(0);
        ObservationAttributeSpec<?> storedSpec = view.attributes().values().keySet().iterator().next();
        Object storedValue = view.attributes().values().get(storedSpec);
        assertSame(redacted, storedSpec);
        assertEquals("[REDACTED]", formatAttribute(storedSpec, storedValue));
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
    void reportsObservationCallbackFailuresAndPreservesBusinessFailures() {
        ObservationSpec spec = ObservationSpec.builder("request").build();
        List<Throwable> failures = new ArrayList<>();
        IllegalStateException filterFailure = new IllegalStateException("filter failure");
        IllegalStateException handlerFailure = new IllegalStateException("handler failure");
        IllegalStateException businessFailure = new IllegalStateException("business failure");
        ObservationHandler handler = new ObservationHandler() {
            @Override
            public void onError(ObservationView view) {
                throw handlerFailure;
            }

            @Override
            public void onStop(ObservationView view) {
                throw handlerFailure;
            }
        };
        try (var registry = ObservationRegistryBuilder.builder()
                .failureHandler((observed, failure) -> {
                    assertEquals(spec, observed);
                    failures.add(failure);
                })
                .filter(start -> {
                    throw filterFailure;
                })
                .handler(handler)
                .build()) {
            registry.register(spec);
            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
                try (Observation observation = registry.start(spec)) {
                    observation.error(businessFailure);
                    throw businessFailure;
                }
            });
            assertSame(businessFailure, thrown);
            assertEquals(List.of(filterFailure, handlerFailure, handlerFailure), failures);
        }
    }

    @Test
    void doesNotSilentlyDiscardJvmErrorsFromObservationCallbacks() {
        ObservationSpec spec = ObservationSpec.builder("request").build();
        AssertionError failure = new AssertionError("fatal callback failure");
        try (var registry = ObservationRegistryBuilder.builder()
                .predicate(start -> {
                    throw failure;
                })
                .build()) {
            registry.register(spec);
            assertSame(failure, assertThrows(AssertionError.class, () -> registry.start(spec)));
        }
    }

    @Test
    void isolatesFailureReporterErrorsWithoutRecursiveReporting() {
        ObservationSpec spec = ObservationSpec.builder("request").build();
        AtomicInteger reports = new AtomicInteger();
        try (var registry = ObservationRegistryBuilder.builder()
                .predicate(start -> {
                    throw new IllegalStateException("predicate failure");
                })
                .failureHandler((observed, failure) -> {
                    reports.incrementAndGet();
                    throw new IllegalStateException("reporter failure");
                })
                .build()) {
            registry.register(spec);
            registry.start(spec).stop();
            assertEquals(1, reports.get());
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
    void selectsHealthAggregationByGroupIncludingEmptyGroups() {
        HealthAggregationPolicy worst = HealthAggregationPolicy.worstStatus();
        HealthRegistry registry = HealthRegistryBuilder.builder(Runnable::run)
                .register("dependency", () -> HealthResult.of(HealthStatus.DOWN))
                .include(HealthGroup.LIVENESS, "dependency")
                .include(HealthGroup.READINESS, "dependency")
                .aggregationPolicy((group, results) -> {
                    if (results.isEmpty()) {
                        return HealthStatus.UNKNOWN;
                    }
                    return group == HealthGroup.LIVENESS ? HealthStatus.UP : worst.aggregate(group, results);
                })
                .build();

        assertEquals(HealthStatus.UP, registry.evaluate(HealthGroup.LIVENESS).getStatus());
        assertEquals(HealthStatus.DOWN, registry.evaluate(HealthGroup.READINESS).getStatus());
        HealthRegistry empty = HealthRegistryBuilder.builder(Runnable::run)
                .aggregationPolicy((group, results) -> HealthStatus.UNKNOWN)
                .build();
        assertEquals(HealthStatus.UNKNOWN, empty.evaluate(HealthGroup.READINESS).getStatus());
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

    @Test
    void enforcesHealthTimeoutWhenConfiguredExecutorRunsInline() throws Exception {
        ExecutorService evaluator = Executors.newSingleThreadExecutor();
        CountDownLatch contributorStarted = new CountDownLatch(1);
        CountDownLatch contributorInterrupted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            HealthRegistry registry = HealthRegistryBuilder.builder(Runnable::run)
                    .timeout(Duration.ofMillis(100))
                    .register("slow", () -> {
                        contributorStarted.countDown();
                        try {
                            release.await();
                        } catch (InterruptedException failure) {
                            contributorInterrupted.countDown();
                            Thread.currentThread().interrupt();
                        }
                        return HealthResult.of(HealthStatus.UP);
                    })
                    .build();

            var evaluation = evaluator.submit(() -> registry.evaluate(HealthGroup.AGGREGATE));

            assertTrue(contributorStarted.await(1L, TimeUnit.SECONDS));
            assertEquals(HealthStatus.DOWN, evaluation.get(1L, TimeUnit.SECONDS).getStatus());
            assertTrue(contributorInterrupted.await(1L, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            evaluator.shutdownNow();
        }
    }

    @Test
    void interruptsTimedOutHealthChecksWithoutMarkingEarlierCompletionAsTimedOut() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch slowInterrupted = new CountDownLatch(1);
        try {
            HealthRegistry registry = HealthRegistryBuilder.builder(executor)
                    .timeout(Duration.ofMillis(150))
                    .register("slow", () -> {
                        try {
                            new CountDownLatch(1).await();
                        } catch (InterruptedException failure) {
                            slowInterrupted.countDown();
                            Thread.currentThread().interrupt();
                        }
                        return HealthResult.of(HealthStatus.UP);
                    })
                    .register("fast", () -> HealthResult.of(HealthStatus.UP))
                    .build();

            HealthResult result = registry.evaluate(HealthGroup.AGGREGATE);

            HealthResult slowResult = (HealthResult) result.getDetails().get("slow").orElseThrow();
            HealthResult fastResult = (HealthResult) result.getDetails().get("fast").orElseThrow();
            assertEquals(HealthStatus.DOWN, slowResult.getStatus());
            assertEquals(HealthStatus.UP, fastResult.getStatus());
            assertTrue(slowInterrupted.await(1L, TimeUnit.SECONDS));
        } finally {
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

    private static <T> String formatAttribute(ObservationAttributeSpec<T> spec, Object value) {
        return spec.formatValue(spec.valueType().cast(value));
    }
}
