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
import com.lamprism.luxspec.observability.metric.GaugeSpec;
import com.lamprism.luxspec.observability.metric.MetricRegistry;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Objects;
import java.util.function.ToLongFunction;

/**
 * Registers live, daemon, peak, and total-started JVM thread metrics.
 *
 * @author RollW
 */
public class JvmThreadMetricSet implements MetricSet {
    private final ThreadMXBean threads;

    /**
     * Creates a thread set from the current JVM.
     */
    public JvmThreadMetricSet() {
        this(ManagementFactory.getThreadMXBean());
    }

    /**
     * Creates a thread set from an explicit bean.
     *
     * @param threads the thread bean
     */
    public JvmThreadMetricSet(ThreadMXBean threads) {
        this.threads = Objects.requireNonNull(threads, "threads");
    }

    @Override
    public void register(MetricRegistry registry) {
        MetricRegistry nonNullRegistry = Objects.requireNonNull(registry, "registry");
        registerGauge(nonNullRegistry, "jvm.threads.live", "Live JVM threads", ThreadMXBean::getThreadCount);
        registerGauge(nonNullRegistry, "jvm.threads.daemon", "Daemon JVM threads", ThreadMXBean::getDaemonThreadCount);
        registerGauge(nonNullRegistry, "jvm.threads.peak", "Peak JVM threads", ThreadMXBean::getPeakThreadCount);
        FunctionCounterSpec<ThreadMXBean> started = FunctionCounterSpec
                .builder("jvm.threads.started", ThreadMXBean.class)
                .description("Total JVM threads started")
                .baseUnit("threads")
                .reader(ThreadMXBean::getTotalStartedThreadCount)
                .build();
        MetricRegistrationSupport.register(nonNullRegistry, started, threads);
    }

    private void registerGauge(
            MetricRegistry registry,
            String name,
            String description,
            ToLongFunction<ThreadMXBean> reader
    ) {
        GaugeSpec<ThreadMXBean> spec = GaugeSpec.builder(name, ThreadMXBean.class)
                .description(description)
                .baseUnit("threads")
                .reader(source -> reader.applyAsLong(source))
                .build();
        MetricRegistrationSupport.register(registry, spec, threads);
    }
}
