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

import com.lamprism.luxspec.observability.metric.GaugeSpec;
import com.lamprism.luxspec.observability.metric.MetricCardinalityPolicy;
import com.lamprism.luxspec.observability.metric.MetricDimensionSpec;
import com.lamprism.luxspec.observability.metric.MetricRegistry;
import org.jspecify.annotations.Nullable;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * Registers heap, non-heap, and memory-pool usage metrics.
 *
 * @author RollW
 */
public class JvmMemoryMetricSet implements MetricSet {
    private static final int MAXIMUM_POOL_BINDINGS = 64;
    private final MemoryMXBean memory;
    private final List<MemoryPoolMXBean> memoryPools;

    /**
     * Creates a memory set from the current JVM management beans.
     */
    public JvmMemoryMetricSet() {
        this(ManagementFactory.getMemoryMXBean(), ManagementFactory.getMemoryPoolMXBeans());
    }

    /**
     * Creates a memory set from explicit management beans.
     *
     * @param memory      the memory bean
     * @param memoryPools the memory-pool beans
     */
    public JvmMemoryMetricSet(
            MemoryMXBean memory,
            Collection<? extends MemoryPoolMXBean> memoryPools
    ) {
        this.memory = Objects.requireNonNull(memory, "memory");
        Objects.requireNonNull(memoryPools, "memoryPools");
        this.memoryPools = memoryPools.stream()
                .map(pool -> Objects.requireNonNull(pool, "memory pool"))
                .toList();
    }

    @Override
    public void register(MetricRegistry registry) {
        MetricRegistry nonNullRegistry = Objects.requireNonNull(registry, "registry");
        registerMemoryMetric(nonNullRegistry, "jvm.memory.heap.used", "Heap memory used", MemoryMXBean::getHeapMemoryUsage, MemoryUsage::getUsed);
        registerMemoryMetric(nonNullRegistry, "jvm.memory.heap.committed", "Heap memory committed", MemoryMXBean::getHeapMemoryUsage, MemoryUsage::getCommitted);
        registerMemoryMetric(nonNullRegistry, "jvm.memory.heap.max", "Heap memory maximum", MemoryMXBean::getHeapMemoryUsage, MemoryUsage::getMax);
        registerMemoryMetric(nonNullRegistry, "jvm.memory.nonheap.used", "Non-heap memory used", MemoryMXBean::getNonHeapMemoryUsage, MemoryUsage::getUsed);
        registerMemoryMetric(nonNullRegistry, "jvm.memory.nonheap.committed", "Non-heap memory committed", MemoryMXBean::getNonHeapMemoryUsage, MemoryUsage::getCommitted);
        registerMemoryMetric(nonNullRegistry, "jvm.memory.nonheap.max", "Non-heap memory maximum", MemoryMXBean::getNonHeapMemoryUsage, MemoryUsage::getMax);

        MetricDimensionSpec<String> poolDimension = MetricRegistrationSupport.dimension("pool");
        GaugeSpec<MemoryPoolMXBean> used = poolGauge(
                "jvm.memory.pool.used",
                "Memory-pool memory used",
                MemoryUsage::getUsed,
                poolDimension
        );
        GaugeSpec<MemoryPoolMXBean> committed = poolGauge(
                "jvm.memory.pool.committed",
                "Memory-pool memory committed",
                MemoryUsage::getCommitted,
                poolDimension
        );
        GaugeSpec<MemoryPoolMXBean> max = poolGauge(
                "jvm.memory.pool.max",
                "Memory-pool memory maximum",
                MemoryUsage::getMax,
                poolDimension
        );
        for (MemoryPoolMXBean pool : memoryPools) {
            String poolName = pool.getName();
            MetricRegistrationSupport.register(nonNullRegistry, used, poolDimension, poolName, pool);
            MetricRegistrationSupport.register(nonNullRegistry, committed, poolDimension, poolName, pool);
            MetricRegistrationSupport.register(nonNullRegistry, max, poolDimension, poolName, pool);
        }
    }

    private void registerMemoryMetric(
            MetricRegistry registry,
            String name,
            String description,
            Function<MemoryMXBean, MemoryUsage> usageReader,
            ToLongFunction<MemoryUsage> reader
    ) {
        GaugeSpec<MemoryMXBean> spec = GaugeSpec.builder(name, MemoryMXBean.class)
                .description(description)
                .baseUnit("bytes")
                .reader(source -> value(usageReader.apply(source), reader))
                .build();
        MetricRegistrationSupport.register(registry, spec, memory);
    }

    private static GaugeSpec<MemoryPoolMXBean> poolGauge(
            String name,
            String description,
            ToLongFunction<MemoryUsage> reader,
            MetricDimensionSpec<String> dimension
    ) {
        return GaugeSpec.builder(name, MemoryPoolMXBean.class)
                .description(description)
                .baseUnit("bytes")
                .dimension(dimension)
                .cardinality(MetricCardinalityPolicy.bounded(MAXIMUM_POOL_BINDINGS))
                .reader(source -> value(source.getUsage(), reader))
                .build();
    }

    private static @Nullable Long value(
            @Nullable MemoryUsage usage,
            ToLongFunction<MemoryUsage> reader
    ) {
        if (usage == null) {
            return null;
        }
        long value = reader.applyAsLong(usage);
        return value < 0L ? null : value;
    }
}
