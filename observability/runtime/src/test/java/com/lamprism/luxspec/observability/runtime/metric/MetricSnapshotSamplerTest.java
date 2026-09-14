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

package com.lamprism.luxspec.observability.runtime.metric;

import com.lamprism.luxspec.observability.metric.MetricRegistry;
import com.lamprism.luxspec.observability.metric.MetricSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricSnapshotSamplerTest {
    @Test
    void startsOnePeriodicTaskAndCancelsItWithoutShuttingDownTheScheduler() {
        List<MetricSnapshot> snapshots = new ArrayList<>();
        ManualScheduledExecutor scheduler = new ManualScheduledExecutor();
        try (MetricRegistry registry = MetricRegistryBuilder.builder().build();
             MetricSnapshotSampler sampler = new MetricSnapshotSampler(
                     registry,
                     snapshots::add,
                     scheduler,
                     Duration.ofSeconds(1)
             )) {
            sampler.start();
            sampler.start();
            scheduler.runScheduled();

            assertTrue(sampler.isStarted());
            assertEquals(1, snapshots.size());

            sampler.close();
            scheduler.runScheduled();
            assertTrue(sampler.isClosed());
            assertEquals(1, snapshots.size());
            assertFalse(scheduler.isShutdown());
        } finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    void exposesScheduledFailuresAndContinuesWithLaterSamples() {
        List<MetricSnapshot> snapshots = new ArrayList<>();
        RuntimeException failure = new IllegalStateException("sink failure");
        boolean[] failNext = {true};
        ManualScheduledExecutor scheduler = new ManualScheduledExecutor();
        try (MetricRegistry registry = MetricRegistryBuilder.builder().build();
             MetricSnapshotSampler sampler = new MetricSnapshotSampler(
                     registry,
                     snapshot -> {
                         if (failNext[0]) {
                             failNext[0] = false;
                             throw failure;
                         }
                         snapshots.add(snapshot);
                     },
                     scheduler,
                     Duration.ofSeconds(1)
             )) {
            sampler.start();
            scheduler.runScheduled();
            assertSame(failure, sampler.getLastFailure());

            scheduler.runScheduled();
            assertNull(sampler.getLastFailure());
            assertEquals(1, snapshots.size());
        } finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    void rejectsInvalidIntervalsAndSamplingAfterClose() {
        ManualScheduledExecutor scheduler = new ManualScheduledExecutor();
        try (MetricRegistry registry = MetricRegistryBuilder.builder().build()) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new MetricSnapshotSampler(
                            registry,
                            snapshot -> {
                            },
                            scheduler,
                            Duration.ZERO
                    )
            );

            MetricSnapshotSampler sampler = new MetricSnapshotSampler(
                    registry,
                    snapshot -> {
                    },
                    scheduler,
                    Duration.ofSeconds(1)
            );
            sampler.close();
            assertThrows(IllegalStateException.class, sampler::sampleNow);
            assertThrows(IllegalStateException.class, sampler::start);
        } finally {
            scheduler.shutdownNow();
        }
    }

    private static final class ManualScheduledExecutor extends AbstractExecutorService
            implements ScheduledExecutorService {
        private Runnable scheduledCommand;
        private ManualScheduledFuture scheduledFuture;
        private boolean shutdown;

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(
                Runnable command,
                long initialDelay,
                long delay,
                TimeUnit unit
        ) {
            if (shutdown) {
                throw new RejectedExecutionException("scheduler is shut down");
            }
            if (scheduledCommand != null) {
                throw new IllegalStateException("only one scheduled command is supported");
            }
            scheduledCommand = command;
            scheduledFuture = new ManualScheduledFuture();
            return scheduledFuture;
        }

        private void runScheduled() {
            if (scheduledCommand == null || scheduledFuture.isCancelled()) {
                return;
            }
            scheduledCommand.run();
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <V> ScheduledFuture<V> schedule(
                Callable<V> callable,
                long delay,
                TimeUnit unit
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(
                Runnable command,
                long initialDelay,
                long period,
                TimeUnit unit
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
            }
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return shutdown;
        }
    }

    private static final class ManualScheduledFuture implements ScheduledFuture<Object> {
        private boolean cancelled;

        @Override
        public long getDelay(TimeUnit unit) {
            return 0L;
        }

        @Override
        public int compareTo(Delayed other) {
            return 0;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelled = true;
            return true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return cancelled;
        }

        @Override
        public Object get() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object get(long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }
    }
}
