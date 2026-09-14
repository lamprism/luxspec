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
import com.lamprism.luxspec.observability.metric.MetricSnapshotSink;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Periodically captures a metric registry and forwards each snapshot to a sink.
 *
 * <p>The scheduler is supplied by the caller and remains caller-owned. The sampler owns only its
 * scheduled task. Use {@link #start()} and {@link #close()} to control that task; closing the
 * sampler does not shut down the supplied scheduler. An already running capture is allowed to
 * finish because cancellation is non-interrupting.</p>
 *
 * <p>Scheduled sampling uses fixed delay, so one scheduled capture is not started until the
 * previous capture has returned. A runtime failure from a scheduled capture is retained in
 * {@link #getLastFailure()} and later scheduled captures continue. Direct calls to
 * {@link #sampleNow()} propagate failures to the caller.</p>
 *
 * @author RollW
 */
public final class MetricSnapshotSampler implements AutoCloseable {
    private final MetricRegistry registry;
    private final MetricSnapshotSink sink;
    private final ScheduledExecutorService scheduler;
    private final long intervalNanos;
    private final Object samplingMonitor = new Object();
    private volatile boolean started;
    private volatile boolean closed;
    private volatile @Nullable RuntimeException lastFailure;
    private @Nullable ScheduledFuture<?> scheduledTask;

    /**
     * Creates a sampler with an application-owned scheduler.
     *
     * @param registry  the registry to capture
     * @param sink      the snapshot consumer
     * @param scheduler the scheduler used for periodic capture
     * @param interval  the fixed delay between completed captures
     */
    public MetricSnapshotSampler(
            MetricRegistry registry,
            MetricSnapshotSink sink,
            ScheduledExecutorService scheduler,
            Duration interval
    ) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.sink = Objects.requireNonNull(sink, "sink");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.intervalNanos = intervalNanos(interval);
    }

    /**
     * Starts periodic capture.
     *
     * <p>The first capture is scheduled immediately. Repeated calls do not create duplicate
     * scheduled tasks.</p>
     *
     * @throws IllegalStateException when the sampler has been closed
     * @throws RuntimeException      when the scheduler rejects the task
     */
    public synchronized void start() {
        requireOpen();
        if (started) {
            return;
        }
        scheduledTask = scheduler.scheduleWithFixedDelay(
                this::runScheduledSample,
                0L,
                intervalNanos,
                TimeUnit.NANOSECONDS
        );
        started = true;
    }

    /**
     * Captures and forwards one snapshot immediately.
     *
     * <p>This method may be used without starting periodic capture. Concurrent direct and
     * scheduled captures are serialized by the sampler.</p>
     *
     * @throws IllegalStateException when the sampler has been closed
     * @throws RuntimeException      when registry capture or sink delivery fails
     */
    public void sampleNow() {
        requireOpen();
        synchronized (samplingMonitor) {
            requireOpen();
            captureAndDeliver();
            lastFailure = null;
        }
    }

    /**
     * Reports whether periodic capture has been started.
     *
     * @return true after a scheduled task has been accepted
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Reports whether this sampler has been closed.
     *
     * @return true after close has been called
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Returns the most recent scheduled sampling failure.
     *
     * <p>A successful capture clears this value. Direct {@link #sampleNow()} failures are thrown
     * to their caller and are not stored here.</p>
     *
     * @return the most recent scheduled failure, or null when none is recorded
     */
    public @Nullable RuntimeException getLastFailure() {
        return lastFailure;
    }

    /**
     * Cancels periodic capture without shutting down the caller-owned scheduler.
     *
     * <p>A capture already in progress is not interrupted and may finish after this method
     * returns.</p>
     */
    @Override
    public void close() {
        ScheduledFuture<?> task;
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            task = scheduledTask;
            scheduledTask = null;
        }
        if (task != null) {
            task.cancel(false);
        }
    }

    private void runScheduledSample() {
        synchronized (samplingMonitor) {
            if (closed) {
                return;
            }
            try {
                captureAndDeliver();
                lastFailure = null;
            } catch (RuntimeException failure) {
                if (!closed) {
                    lastFailure = failure;
                }
            }
        }
    }

    private void captureAndDeliver() {
        requireOpen();
        MetricSnapshot snapshot = registry.snapshot();
        sink.accept(snapshot);
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Metric snapshot sampler is closed");
        }
    }

    private static long intervalNanos(Duration interval) {
        Duration nonNullInterval = Objects.requireNonNull(interval, "interval");
        if (nonNullInterval.isZero() || nonNullInterval.isNegative()) {
            throw new IllegalArgumentException("interval must be positive");
        }
        try {
            return nonNullInterval.toNanos();
        } catch (ArithmeticException failure) {
            throw new IllegalArgumentException("interval is too large", failure);
        }
    }
}
