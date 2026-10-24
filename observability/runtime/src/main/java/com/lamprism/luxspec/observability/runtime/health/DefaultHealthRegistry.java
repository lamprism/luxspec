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

package com.lamprism.luxspec.observability.runtime.health;

import com.lamprism.luxspec.observability.health.HealthContributor;
import com.lamprism.luxspec.observability.health.HealthDetailSet;
import com.lamprism.luxspec.observability.health.HealthGroup;
import com.lamprism.luxspec.observability.health.HealthRegistry;
import com.lamprism.luxspec.observability.health.HealthResult;
import com.lamprism.luxspec.observability.health.HealthStatus;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Evaluates registered health contributors with explicit concurrency bounds.
 *
 * @author RollW
 */
final class DefaultHealthRegistry implements HealthRegistry {
    private final Executor executor;
    private final Map<String, HealthContributor> contributors;
    private final Map<HealthGroup, List<String>> groups;
    private final @Nullable Long timeoutNanos;

    DefaultHealthRegistry(
            Executor executor,
            Map<String, HealthContributor> contributors,
            Map<HealthGroup, ? extends Set<String>> groups,
            @Nullable Duration timeout
    ) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.contributors = Map.copyOf(contributors);
        this.groups = new EnumMap<>(HealthGroup.class);
        for (Map.Entry<HealthGroup, ? extends java.util.Set<String>> entry : groups.entrySet()) {
            this.groups.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        this.timeoutNanos = timeout == null ? null : timeout.toNanos();
    }

    @Override
    public HealthResult evaluate(HealthGroup group) {
        HealthGroup nonNullGroup = Objects.requireNonNull(group, "group");
        List<String> names = Objects.requireNonNull(groups.get(nonNullGroup), "health group");
        if (names.isEmpty()) {
            return HealthResult.of(HealthStatus.UP);
        }
        Map<String, HealthTask> tasks = new LinkedHashMap<>();
        Map<String, Long> submittedAt = new LinkedHashMap<>();
        Map<String, HealthResult> results = new LinkedHashMap<>();
        for (String name : names) {
            try {
                long startedAt = System.nanoTime();
                HealthContributor contributor = Objects.requireNonNull(
                        contributors.get(name),
                        "health contributor"
                );
                HealthTask task = new HealthTask(() ->
                        new CompletedHealthResult(contribute(contributor), System.nanoTime())
                );
                submit(task);
                tasks.put(name, task);
                submittedAt.put(name, startedAt);
            } catch (RuntimeException failure) {
                results.put(name, HealthResult.of(HealthStatus.UNKNOWN));
            }
        }
        for (Map.Entry<String, HealthTask> entry : tasks.entrySet()) {
            long startedAt = Objects.requireNonNull(submittedAt.get(entry.getKey()), "submitted health task");
            results.putIfAbsent(entry.getKey(), resolve(entry.getValue(), startedAt));
            if (Thread.currentThread().isInterrupted()) {
                tasks.values().forEach(task -> task.cancel(true));
                break;
            }
        }
        for (String name : names) {
            results.putIfAbsent(name, HealthResult.of(HealthStatus.UNKNOWN));
        }
        return aggregate(results);
    }

    private HealthResult resolve(HealthTask task, long submittedAt) {
        try {
            if (timeoutNanos == null) {
                return normalize(task.get().result());
            }
            long elapsedNanos = System.nanoTime() - submittedAt;
            long remainingNanos = timeoutNanos - elapsedNanos;
            if (remainingNanos <= 0L && !task.isDone()) {
                task.cancel(true);
                return HealthResult.of(HealthStatus.DOWN);
            }
            CompletedHealthResult completed = remainingNanos <= 0L
                    ? task.get()
                    : task.get(remainingNanos, TimeUnit.NANOSECONDS);
            if (completed.completedAtNanos() - submittedAt > timeoutNanos) {
                return HealthResult.of(HealthStatus.DOWN);
            }
            return normalize(completed.result());
        } catch (TimeoutException failure) {
            task.cancel(true);
            return HealthResult.of(HealthStatus.DOWN);
        } catch (InterruptedException failure) {
            task.cancel(true);
            Thread.currentThread().interrupt();
            return HealthResult.of(HealthStatus.UNKNOWN);
        } catch (CancellationException failure) {
            return HealthResult.of(HealthStatus.UNKNOWN);
        } catch (ExecutionException failure) {
            return HealthResult.of(task.wasRejected() ? HealthStatus.UNKNOWN : HealthStatus.DOWN);
        }
    }

    private void submit(HealthTask task) {
        if (timeoutNanos == null) {
            executor.execute(task);
            return;
        }
        Thread submittingThread = Thread.currentThread();
        try {
            executor.execute(() -> {
                if (Thread.currentThread() == submittingThread) {
                    ForkJoinPool.commonPool().execute(task);
                } else {
                    task.run();
                }
            });
        } catch (RuntimeException failure) {
            task.fail(failure);
        }
    }

    private static HealthResult contribute(HealthContributor contributor) {
        return contributor.contribute();
    }

    private static HealthResult normalize(@Nullable HealthResult result) {
        if (result == null) {
            return HealthResult.of(HealthStatus.UNKNOWN);
        }
        return result;
    }

    private record CompletedHealthResult(@Nullable HealthResult result, long completedAtNanos) {
    }

    private static final class HealthTask extends FutureTask<CompletedHealthResult> {
        private volatile boolean rejected;

        private HealthTask(Callable<CompletedHealthResult> task) {
            super(task);
        }

        private void fail(RuntimeException failure) {
            rejected = true;
            setException(failure);
        }

        private boolean wasRejected() {
            return rejected;
        }
    }

    private static HealthResult aggregate(Map<String, HealthResult> results) {
        HealthStatus status = HealthStatus.UP;
        HealthDetailSet.Builder details = HealthDetailSet.builder();
        for (Map.Entry<String, HealthResult> entry : results.entrySet()) {
            HealthResult result = entry.getValue();
            status = moreSevere(status, result.getStatus());
            details.put(entry.getKey(), result);
        }
        return HealthResult.of(status, details.build());
    }

    private static HealthStatus moreSevere(HealthStatus current, HealthStatus candidate) {
        if (severity(candidate) > severity(current)) {
            return candidate;
        }
        return current;
    }

    private static int severity(HealthStatus status) {
        return switch (status) {
            case UP -> 0;
            case UNKNOWN -> 1;
            case OUT_OF_SERVICE -> 2;
            case DOWN -> 3;
        };
    }
}
