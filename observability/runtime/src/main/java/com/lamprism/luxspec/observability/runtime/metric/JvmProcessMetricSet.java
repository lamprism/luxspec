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
import com.lamprism.luxspec.observability.metric.MetricRegistry;
import com.lamprism.luxspec.observability.metric.TimeGaugeSpec;
import org.jspecify.annotations.Nullable;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.util.Objects;

/**
 * Registers JVM uptime, processor count, load, and supported Unix file-descriptor metrics.
 *
 * @author RollW
 */
public class JvmProcessMetricSet implements MetricSet {
    private final RuntimeMXBean runtime;
    private final OperatingSystemMXBean operatingSystem;

    /**
     * Creates a process set from the current JVM.
     */
    public JvmProcessMetricSet() {
        this(ManagementFactory.getRuntimeMXBean(), ManagementFactory.getOperatingSystemMXBean());
    }

    /**
     * Creates a process set from explicit management beans.
     *
     * @param runtime         the runtime bean
     * @param operatingSystem the operating-system bean
     */
    public JvmProcessMetricSet(
            RuntimeMXBean runtime,
            OperatingSystemMXBean operatingSystem
    ) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.operatingSystem = Objects.requireNonNull(operatingSystem, "operatingSystem");
    }

    @Override
    public void register(MetricRegistry registry) {
        MetricRegistry nonNullRegistry = Objects.requireNonNull(registry, "registry");
        TimeGaugeSpec<RuntimeMXBean> uptime = TimeGaugeSpec
                .builder("jvm.uptime", RuntimeMXBean.class)
                .description("JVM uptime")
                .baseUnit("milliseconds")
                .reader(source -> Duration.ofMillis(source.getUptime()))
                .build();
        MetricRegistrationSupport.register(nonNullRegistry, uptime, runtime);

        GaugeSpec<OperatingSystemMXBean> processors = GaugeSpec
                .builder("jvm.processors.available", OperatingSystemMXBean.class)
                .description("Available processors")
                .baseUnit("processors")
                .reader(source -> source.getAvailableProcessors())
                .build();
        MetricRegistrationSupport.register(nonNullRegistry, processors, operatingSystem);

        GaugeSpec<OperatingSystemMXBean> systemLoad = GaugeSpec
                .builder("jvm.system.load", OperatingSystemMXBean.class)
                .description("System CPU load")
                .reader(JvmProcessMetricSet::systemLoad)
                .build();
        MetricRegistrationSupport.register(nonNullRegistry, systemLoad, operatingSystem);

        GaugeSpec<OperatingSystemMXBean> processLoad = GaugeSpec
                .builder("jvm.process.cpu.load", OperatingSystemMXBean.class)
                .description("Process CPU load")
                .reader(JvmProcessMetricSet::processLoad)
                .build();
        MetricRegistrationSupport.register(nonNullRegistry, processLoad, operatingSystem);

        if (operatingSystem instanceof com.sun.management.OperatingSystemMXBean) {
            GaugeSpec<OperatingSystemMXBean> openDescriptors = GaugeSpec
                    .builder("jvm.unix.file.descriptors.open", OperatingSystemMXBean.class)
                    .description("Open Unix file descriptors")
                    .baseUnit("file descriptors")
                    .reader(JvmProcessMetricSet::openFileDescriptors)
                    .build();
            MetricRegistrationSupport.register(nonNullRegistry, openDescriptors, operatingSystem);

            GaugeSpec<OperatingSystemMXBean> maximumDescriptors = GaugeSpec
                    .builder("jvm.unix.file.descriptors.max", OperatingSystemMXBean.class)
                    .description("Maximum Unix file descriptors")
                    .baseUnit("file descriptors")
                    .reader(JvmProcessMetricSet::maximumFileDescriptors)
                    .build();
            MetricRegistrationSupport.register(nonNullRegistry, maximumDescriptors, operatingSystem);
        }
    }

    private static @Nullable Double systemLoad(OperatingSystemMXBean source) {
        double load = source.getSystemLoadAverage();
        return load < 0.0d ? null : load;
    }

    private static @Nullable Double processLoad(OperatingSystemMXBean source) {
        if (!(source instanceof com.sun.management.OperatingSystemMXBean extended)) {
            return null;
        }
        double load = extended.getProcessCpuLoad();
        return load < 0.0d ? null : load;
    }

    private static @Nullable Long openFileDescriptors(OperatingSystemMXBean source) {
        if (!(source instanceof com.sun.management.UnixOperatingSystemMXBean extended)) {
            return null;
        }
        long count = extended.getOpenFileDescriptorCount();
        return count < 0L ? null : count;
    }

    private static @Nullable Long maximumFileDescriptors(OperatingSystemMXBean source) {
        if (!(source instanceof com.sun.management.UnixOperatingSystemMXBean extended)) {
            return null;
        }
        long count = extended.getMaxFileDescriptorCount();
        return count < 0L ? null : count;
    }
}
