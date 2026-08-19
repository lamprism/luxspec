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

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.ToLongFunction;

/**
 * Registers buffer-pool count, used bytes, and capacity per pool.
 *
 * @author RollW
 */
public class JvmBufferPoolMetricSet implements MetricSet {
    private static final int MAXIMUM_POOL_BINDINGS = 32;
    private final List<BufferPoolMXBean> pools;

    /**
     * Creates a buffer-pool set from the current JVM.
     */
    public JvmBufferPoolMetricSet() {
        this(ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class));
    }

    /**
     * Creates a buffer-pool set from explicit beans.
     *
     * @param pools the buffer-pool beans
     */
    public JvmBufferPoolMetricSet(Collection<? extends BufferPoolMXBean> pools) {
        Objects.requireNonNull(pools, "pools");
        this.pools = pools.stream()
                .map(pool -> Objects.requireNonNull(pool, "buffer pool"))
                .toList();
    }

    @Override
    public void register(MetricRegistry registry) {
        MetricRegistry nonNullRegistry = Objects.requireNonNull(registry, "registry");
        MetricDimensionSpec<String> poolDimension = MetricRegistrationSupport.dimension("pool");
        GaugeSpec<BufferPoolMXBean> count = gauge(
                "jvm.buffer.count",
                "Buffer-pool count",
                BufferPoolMXBean::getCount,
                poolDimension,
                "buffers"
        );
        GaugeSpec<BufferPoolMXBean> used = gauge(
                "jvm.buffer.used",
                "Buffer-pool used memory",
                BufferPoolMXBean::getMemoryUsed,
                poolDimension,
                "bytes"
        );
        GaugeSpec<BufferPoolMXBean> capacity = gauge(
                "jvm.buffer.capacity",
                "Buffer-pool capacity",
                BufferPoolMXBean::getTotalCapacity,
                poolDimension,
                "bytes"
        );
        for (BufferPoolMXBean pool : pools) {
            String name = Objects.requireNonNull(pool.getName(), "buffer pool name");
            MetricRegistrationSupport.register(nonNullRegistry, count, poolDimension, name, pool);
            MetricRegistrationSupport.register(nonNullRegistry, used, poolDimension, name, pool);
            MetricRegistrationSupport.register(nonNullRegistry, capacity, poolDimension, name, pool);
        }
    }

    private static GaugeSpec<BufferPoolMXBean> gauge(
            String name,
            String description,
            ToLongFunction<BufferPoolMXBean> reader,
            MetricDimensionSpec<String> dimension,
            String unit
    ) {
        return GaugeSpec.builder(name, BufferPoolMXBean.class)
                .description(description)
                .baseUnit(unit)
                .dimension(dimension)
                .cardinality(MetricCardinalityPolicy.bounded(MAXIMUM_POOL_BINDINGS))
                .reader(source -> reader.applyAsLong(source))
                .build();
    }
}
