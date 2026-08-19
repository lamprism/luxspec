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

import com.lamprism.luxspec.observability.metric.FunctionCounterSpec;
import com.lamprism.luxspec.observability.metric.MetricCardinalityPolicy;
import com.lamprism.luxspec.observability.metric.MetricDimensionSpec;
import com.lamprism.luxspec.observability.metric.MetricRegistry;
import org.jspecify.annotations.Nullable;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.ToLongFunction;

/**
 * Registers garbage-collection count and collection-time metrics per collector.
 *
 * @author RollW
 */
public class JvmGarbageCollectionMetricSet implements MetricSet {
    private static final int MAXIMUM_COLLECTOR_BINDINGS = 32;
    private final List<GarbageCollectorMXBean> collectors;

    /**
     * Creates a GC set from the current JVM.
     */
    public JvmGarbageCollectionMetricSet() {
        this(ManagementFactory.getGarbageCollectorMXBeans());
    }

    /**
     * Creates a GC set from explicit collector beans.
     *
     * @param collectors the collector beans
     */
    public JvmGarbageCollectionMetricSet(Collection<? extends GarbageCollectorMXBean> collectors) {
        Objects.requireNonNull(collectors, "collectors");
        this.collectors = collectors.stream()
                .map(collector -> Objects.requireNonNull(collector, "collector"))
                .toList();
    }

    @Override
    public void register(MetricRegistry registry) {
        MetricRegistry nonNullRegistry = Objects.requireNonNull(registry, "registry");
        MetricDimensionSpec<String> collectorDimension = MetricRegistrationSupport.dimension("collector");
        FunctionCounterSpec<GarbageCollectorMXBean> count = counter(
                "jvm.gc.collections",
                "Garbage-collection count",
                GarbageCollectorMXBean::getCollectionCount,
                collectorDimension,
                "collections"
        );
        FunctionCounterSpec<GarbageCollectorMXBean> time = counter(
                "jvm.gc.time",
                "Garbage-collection time",
                GarbageCollectorMXBean::getCollectionTime,
                collectorDimension,
                "milliseconds"
        );
        for (GarbageCollectorMXBean collector : collectors) {
            String name = Objects.requireNonNull(collector.getName(), "collector name");
            MetricRegistrationSupport.register(nonNullRegistry, count, collectorDimension, name, collector);
            MetricRegistrationSupport.register(nonNullRegistry, time, collectorDimension, name, collector);
        }
    }

    private static FunctionCounterSpec<GarbageCollectorMXBean> counter(
            String name,
            String description,
            ToLongFunction<GarbageCollectorMXBean> reader,
            MetricDimensionSpec<String> dimension,
            String unit
    ) {
        return FunctionCounterSpec.builder(name, GarbageCollectorMXBean.class)
                .description(description)
                .baseUnit(unit)
                .dimension(dimension)
                .cardinality(MetricCardinalityPolicy.bounded(MAXIMUM_COLLECTOR_BINDINGS))
                .reader(source -> nonNegative(reader.applyAsLong(source)))
                .build();
    }

    private static @Nullable Long nonNegative(long value) {
        return value < 0L ? null : value;
    }
}
