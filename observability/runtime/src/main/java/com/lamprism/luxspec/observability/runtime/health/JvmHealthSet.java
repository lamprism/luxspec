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

import com.lamprism.luxspec.observability.health.HealthDetailSet;
import com.lamprism.luxspec.observability.health.HealthGroup;
import com.lamprism.luxspec.observability.health.HealthResult;
import com.lamprism.luxspec.observability.health.HealthStatus;
import com.sun.management.UnixOperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Objects;

/**
 * Provides opt-in JVM memory, deadlock, and supported Unix file-descriptor health checks.
 *
 * <p>Checks run only when the health registry evaluates them; this set creates no monitoring
 * thread. Unsupported optional management operations are represented as {@link HealthStatus#UNKNOWN}
 * or omitted when the platform capability is not present.</p>
 *
 * @author RollW
 */
public class JvmHealthSet implements HealthSet {
    private final MemoryMXBean memory;
    private final ThreadMXBean threads;
    private final OperatingSystemMXBean operatingSystem;
    private final double memoryThreshold;
    private final double fileDescriptorThreshold;
    private final boolean deadlockDetection;

    /**
     * Creates the default JVM health set.
     */
    public JvmHealthSet() {
        this(
                ManagementFactory.getMemoryMXBean(),
                ManagementFactory.getThreadMXBean(),
                ManagementFactory.getOperatingSystemMXBean(),
                0.95d,
                0.95d,
                true
        );
    }

    /**
     * Creates a JVM health set with explicit thresholds and detection selection.
     *
     * @param memory                  the memory bean
     * @param threads                 the thread bean
     * @param operatingSystem         the operating-system bean
     * @param memoryThreshold         maximum allowed heap usage ratio
     * @param fileDescriptorThreshold maximum allowed file-descriptor usage ratio
     * @param deadlockDetection       whether deadlock detection is registered
     */
    public JvmHealthSet(
            MemoryMXBean memory,
            ThreadMXBean threads,
            OperatingSystemMXBean operatingSystem,
            double memoryThreshold,
            double fileDescriptorThreshold,
            boolean deadlockDetection
    ) {
        this.memory = Objects.requireNonNull(memory, "memory");
        this.threads = Objects.requireNonNull(threads, "threads");
        this.operatingSystem = Objects.requireNonNull(operatingSystem, "operatingSystem");
        this.memoryThreshold = requireRatio(memoryThreshold, "memoryThreshold");
        this.fileDescriptorThreshold = requireRatio(fileDescriptorThreshold, "fileDescriptorThreshold");
        this.deadlockDetection = deadlockDetection;
    }

    /**
     * Starts configuring a JVM health set.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void register(HealthRegistryBuilder builder) {
        HealthRegistryBuilder nonNullBuilder = Objects.requireNonNull(builder, "builder");
        nonNullBuilder
                .register("jvm.memory", this::memory)
                .include(HealthGroup.LIVENESS, "jvm.memory");
        if (deadlockDetection) {
            nonNullBuilder
                    .register("jvm.deadlock", this::deadlock)
                    .include(HealthGroup.LIVENESS, "jvm.deadlock");
        }
        if (supportsFileDescriptors()) {
            nonNullBuilder
                    .register("jvm.file-descriptor", this::fileDescriptors)
                    .include(HealthGroup.LIVENESS, "jvm.file-descriptor");
        }
    }

    private HealthResult memory() {
        MemoryUsage usage = memory.getHeapMemoryUsage();
        long max = usage.getMax();
        if (max <= 0L) {
            return HealthResult.of(
                    HealthStatus.UNKNOWN,
                    HealthDetailSet.builder().put("reason", "heap maximum is unavailable").build()
            );
        }
        double ratio = (double) usage.getUsed() / (double) max;
        HealthStatus status = ratio > memoryThreshold ? HealthStatus.DOWN : HealthStatus.UP;
        return HealthResult.of(
                status,
                HealthDetailSet.builder()
                        .put("used", usage.getUsed())
                        .put("max", max)
                        .put("ratio", ratio)
                        .build()
        );
    }

    private HealthResult deadlock() {
        long[] deadlocked;
        try {
            deadlocked = threads.findDeadlockedThreads();
        } catch (UnsupportedOperationException failure) {
            return HealthResult.of(
                    HealthStatus.UNKNOWN,
                    HealthDetailSet.builder().put("reason", "deadlock detection is unsupported").build()
            );
        }
        int count = deadlocked == null ? 0 : deadlocked.length;
        return HealthResult.of(
                count == 0 ? HealthStatus.UP : HealthStatus.DOWN,
                HealthDetailSet.builder().put("deadlocked", count).build()
        );
    }

    private HealthResult fileDescriptors() {
        UnixOperatingSystemMXBean extended = (UnixOperatingSystemMXBean) operatingSystem;
        long max = extended.getMaxFileDescriptorCount();
        long open = extended.getOpenFileDescriptorCount();
        if (max <= 0L || open < 0L) {
            return HealthResult.of(HealthStatus.UNKNOWN);
        }
        double ratio = (double) open / (double) max;
        HealthStatus status = ratio > fileDescriptorThreshold ? HealthStatus.DOWN : HealthStatus.UP;
        return HealthResult.of(
                status,
                HealthDetailSet.builder()
                        .put("open", open)
                        .put("max", max)
                        .put("ratio", ratio)
                        .build()
        );
    }

    private boolean supportsFileDescriptors() {
        return operatingSystem instanceof UnixOperatingSystemMXBean;
    }

    private static double requireRatio(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0d || value > 1.0d) {
            throw new IllegalArgumentException(name + " must be greater than zero and no greater than one");
        }
        return value;
    }

    /**
     * Builds a configurable JVM health set.
     */
    public static class Builder {
        private MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        private ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        private OperatingSystemMXBean operatingSystem = ManagementFactory.getOperatingSystemMXBean();
        private double memoryThreshold = 0.95d;
        private double fileDescriptorThreshold = 0.95d;
        private boolean deadlockDetection = true;

        public Builder memory(MemoryMXBean memory) {
            this.memory = Objects.requireNonNull(memory, "memory");
            return this;
        }

        public Builder threads(ThreadMXBean threads) {
            this.threads = Objects.requireNonNull(threads, "threads");
            return this;
        }

        public Builder operatingSystem(OperatingSystemMXBean operatingSystem) {
            this.operatingSystem = Objects.requireNonNull(operatingSystem, "operatingSystem");
            return this;
        }

        public Builder memoryThreshold(double threshold) {
            this.memoryThreshold = requireRatio(threshold, "memoryThreshold");
            return this;
        }

        public Builder fileDescriptorThreshold(double threshold) {
            this.fileDescriptorThreshold = requireRatio(threshold, "fileDescriptorThreshold");
            return this;
        }

        public Builder deadlockDetection(boolean enabled) {
            this.deadlockDetection = enabled;
            return this;
        }

        public JvmHealthSet build() {
            return new JvmHealthSet(
                    memory,
                    threads,
                    operatingSystem,
                    memoryThreshold,
                    fileDescriptorThreshold,
                    deadlockDetection
            );
        }
    }
}
