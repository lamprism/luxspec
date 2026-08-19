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

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.util.Objects;

/**
 * Registers loaded, total-loaded, and unloaded class metrics.
 *
 * @author RollW
 */
public class JvmClassLoadingMetricSet implements MetricSet {
    private final ClassLoadingMXBean classLoading;

    /**
     * Creates a class-loading set from the current JVM.
     */
    public JvmClassLoadingMetricSet() {
        this(ManagementFactory.getClassLoadingMXBean());
    }

    /**
     * Creates a class-loading set from an explicit bean.
     *
     * @param classLoading the class-loading bean
     */
    public JvmClassLoadingMetricSet(ClassLoadingMXBean classLoading) {
        this.classLoading = Objects.requireNonNull(classLoading, "classLoading");
    }

    @Override
    public void register(MetricRegistry registry) {
        MetricRegistry nonNullRegistry = Objects.requireNonNull(registry, "registry");
        GaugeSpec<ClassLoadingMXBean> loaded = GaugeSpec
                .builder("jvm.classes.loaded", ClassLoadingMXBean.class)
                .description("Currently loaded JVM classes")
                .baseUnit("classes")
                .reader(ClassLoadingMXBean::getLoadedClassCount)
                .build();
        MetricRegistrationSupport.register(nonNullRegistry, loaded, classLoading);

        FunctionCounterSpec<ClassLoadingMXBean> total = FunctionCounterSpec
                .builder("jvm.classes.loaded.total", ClassLoadingMXBean.class)
                .description("Total JVM classes loaded")
                .baseUnit("classes")
                .reader(ClassLoadingMXBean::getTotalLoadedClassCount)
                .build();
        MetricRegistrationSupport.register(nonNullRegistry, total, classLoading);

        FunctionCounterSpec<ClassLoadingMXBean> unloaded = FunctionCounterSpec
                .builder("jvm.classes.unloaded", ClassLoadingMXBean.class)
                .description("Total JVM classes unloaded")
                .baseUnit("classes")
                .reader(ClassLoadingMXBean::getUnloadedClassCount)
                .build();
        MetricRegistrationSupport.register(nonNullRegistry, unloaded, classLoading);
    }
}
