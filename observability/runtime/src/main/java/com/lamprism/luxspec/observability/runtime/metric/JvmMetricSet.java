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

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Selects the standard JVM metric domains as one explicit contribution.
 *
 * <p>The individual domains remain public so applications can choose only memory, threads, GC,
 * or another subset. Unsupported optional MXBean capabilities are skipped by their domain.</p>
 *
 * @author RollW
 */
public class JvmMetricSet implements MetricSet {
    private final List<MetricSet> domains;

    /**
     * Selectable JVM metric domains.
     */
    public enum Domain {
        MEMORY,
        THREAD,
        CLASS_LOADING,
        GARBAGE_COLLECTION,
        BUFFER_POOL,
        PROCESS
    }

    /**
     * Creates the complete JVM metric set from the current JVM management beans.
     */
    public JvmMetricSet() {
        this(
                ManagementFactory.getMemoryMXBean(),
                ManagementFactory.getMemoryPoolMXBeans(),
                ManagementFactory.getThreadMXBean(),
                ManagementFactory.getClassLoadingMXBean(),
                ManagementFactory.getGarbageCollectorMXBeans(),
                ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class),
                ManagementFactory.getRuntimeMXBean(),
                ManagementFactory.getOperatingSystemMXBean(),
                EnumSet.allOf(Domain.class)
        );
    }

    /**
     * Creates a complete JVM metric set from explicit management beans.
     *
     * @param memory            the memory bean
     * @param memoryPools       the memory-pool beans
     * @param threads           the thread bean
     * @param classLoading      the class-loading bean
     * @param garbageCollectors the garbage-collector beans
     * @param bufferPools       the buffer-pool beans
     * @param runtime           the runtime bean
     * @param operatingSystem   the operating-system bean
     */
    public JvmMetricSet(
            MemoryMXBean memory,
            Collection<? extends MemoryPoolMXBean> memoryPools,
            ThreadMXBean threads,
            ClassLoadingMXBean classLoading,
            Collection<? extends GarbageCollectorMXBean> garbageCollectors,
            Collection<? extends BufferPoolMXBean> bufferPools,
            RuntimeMXBean runtime,
            OperatingSystemMXBean operatingSystem
    ) {
        this(
                memory,
                memoryPools,
                threads,
                classLoading,
                garbageCollectors,
                bufferPools,
                runtime,
                operatingSystem,
                EnumSet.allOf(Domain.class)
        );
    }

    private JvmMetricSet(
            MemoryMXBean memory,
            Collection<? extends MemoryPoolMXBean> memoryPools,
            ThreadMXBean threads,
            ClassLoadingMXBean classLoading,
            Collection<? extends GarbageCollectorMXBean> garbageCollectors,
            Collection<? extends BufferPoolMXBean> bufferPools,
            RuntimeMXBean runtime,
            OperatingSystemMXBean operatingSystem,
            Set<Domain> selectedDomains
    ) {
        Set<Domain> suppliedDomains = Objects.requireNonNull(selectedDomains, "domains");
        EnumSet<Domain> nonNullDomains = suppliedDomains.isEmpty()
                ? EnumSet.noneOf(Domain.class)
                : EnumSet.copyOf(suppliedDomains);
        if (nonNullDomains.isEmpty()) {
            throw new IllegalArgumentException("At least one JVM metric domain is required");
        }
        List<MetricSet> selected = new ArrayList<>(nonNullDomains.size());
        if (nonNullDomains.contains(Domain.MEMORY)) {
            selected.add(new JvmMemoryMetricSet(memory, memoryPools));
        }
        if (nonNullDomains.contains(Domain.THREAD)) {
            selected.add(new JvmThreadMetricSet(threads));
        }
        if (nonNullDomains.contains(Domain.CLASS_LOADING)) {
            selected.add(new JvmClassLoadingMetricSet(classLoading));
        }
        if (nonNullDomains.contains(Domain.GARBAGE_COLLECTION)) {
            selected.add(new JvmGarbageCollectionMetricSet(garbageCollectors));
        }
        if (nonNullDomains.contains(Domain.BUFFER_POOL)) {
            selected.add(new JvmBufferPoolMetricSet(bufferPools));
        }
        if (nonNullDomains.contains(Domain.PROCESS)) {
            selected.add(new JvmProcessMetricSet(runtime, operatingSystem));
        }
        this.domains = List.copyOf(selected);
    }

    /**
     * Creates a set from selected public JVM metric domains.
     *
     * @param domains the selected domains
     * @return the aggregate JVM metric set
     */
    public static JvmMetricSet of(Collection<? extends MetricSet> domains) {
        Objects.requireNonNull(domains, "domains");
        List<MetricSet> copied = new ArrayList<>(domains.size());
        for (MetricSet domain : domains) {
            copied.add(Objects.requireNonNull(domain, "domain"));
        }
        return new JvmMetricSet(copied);
    }

    private JvmMetricSet(List<MetricSet> domains) {
        this.domains = List.copyOf(domains);
    }

    /**
     * Starts building a selected JVM metric set.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void register(MetricRegistry registry) {
        MetricRegistry nonNullRegistry = Objects.requireNonNull(registry, "registry");
        for (MetricSet domain : domains) {
            domain.register(nonNullRegistry);
        }
    }

    /**
     * Builds a JVM metric set from selected management domains.
     */
    public static class Builder {
        private final EnumSet<Domain> domains = EnumSet.allOf(Domain.class);
        private MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        private Collection<? extends MemoryPoolMXBean> memoryPools = ManagementFactory.getMemoryPoolMXBeans();
        private ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        private ClassLoadingMXBean classLoading = ManagementFactory.getClassLoadingMXBean();
        private Collection<? extends GarbageCollectorMXBean> garbageCollectors =
                ManagementFactory.getGarbageCollectorMXBeans();
        private Collection<? extends BufferPoolMXBean> bufferPools =
                ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);
        private RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        private OperatingSystemMXBean operatingSystem = ManagementFactory.getOperatingSystemMXBean();

        public Builder domain(Domain domain) {
            domains.add(Objects.requireNonNull(domain, "domain"));
            return this;
        }

        public Builder domains(Collection<Domain> selectedDomains) {
            Objects.requireNonNull(selectedDomains, "domains");
            domains.clear();
            for (Domain domain : selectedDomains) {
                domains.add(Objects.requireNonNull(domain, "domain"));
            }
            return this;
        }

        public Builder all() {
            domains.addAll(EnumSet.allOf(Domain.class));
            return this;
        }

        public Builder memory(MemoryMXBean memory) {
            this.memory = Objects.requireNonNull(memory, "memory");
            return this;
        }

        public Builder memoryPools(Collection<? extends MemoryPoolMXBean> memoryPools) {
            this.memoryPools = Objects.requireNonNull(memoryPools, "memoryPools");
            return this;
        }

        public Builder threads(ThreadMXBean threads) {
            this.threads = Objects.requireNonNull(threads, "threads");
            return this;
        }

        public Builder classLoading(ClassLoadingMXBean classLoading) {
            this.classLoading = Objects.requireNonNull(classLoading, "classLoading");
            return this;
        }

        public Builder garbageCollectors(Collection<? extends GarbageCollectorMXBean> collectors) {
            this.garbageCollectors = Objects.requireNonNull(collectors, "garbageCollectors");
            return this;
        }

        public Builder bufferPools(Collection<? extends BufferPoolMXBean> bufferPools) {
            this.bufferPools = Objects.requireNonNull(bufferPools, "bufferPools");
            return this;
        }

        public Builder runtime(RuntimeMXBean runtime) {
            this.runtime = Objects.requireNonNull(runtime, "runtime");
            return this;
        }

        public Builder operatingSystem(OperatingSystemMXBean operatingSystem) {
            this.operatingSystem = Objects.requireNonNull(operatingSystem, "operatingSystem");
            return this;
        }

        public JvmMetricSet build() {
            return new JvmMetricSet(
                    memory,
                    memoryPools,
                    threads,
                    classLoading,
                    garbageCollectors,
                    bufferPools,
                    runtime,
                    operatingSystem,
                    domains
            );
        }
    }
}
