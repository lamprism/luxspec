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

import com.lamprism.luxspec.observability.metric.FunctionCounter;
import com.lamprism.luxspec.observability.metric.FunctionCounterSpec;
import com.lamprism.luxspec.observability.metric.Gauge;
import com.lamprism.luxspec.observability.metric.GaugeSpec;
import com.lamprism.luxspec.observability.metric.MetricDimensionSet;
import com.lamprism.luxspec.observability.metric.MetricDimensionSpec;
import com.lamprism.luxspec.observability.metric.MetricRegistry;
import com.lamprism.luxspec.observability.metric.TimeGauge;
import com.lamprism.luxspec.observability.metric.TimeGaugeSpec;

import java.util.Objects;

/**
 * Package-level registration helpers shared by metric domains.
 *
 * @author RollW
 */
final class MetricRegistrationSupport {
    private MetricRegistrationSupport() {
    }

    static MetricDimensionSpec<String> dimension(String name) {
        return MetricDimensionSpec.string(name).build();
    }

    static <S> Gauge register(
            MetricRegistry registry,
            GaugeSpec<S> spec,
            S source
    ) {
        MetricRegistry nonNullRegistry = Objects.requireNonNull(registry, "registry");
        nonNullRegistry.register(spec);
        return nonNullRegistry.obtain(spec.bind(MetricDimensionSet.empty(), Objects.requireNonNull(source, "source")));
    }

    static <S> Gauge register(
            MetricRegistry registry,
            GaugeSpec<S> spec,
            MetricDimensionSpec<String> dimension,
            String dimensionValue,
            S source
    ) {
        MetricRegistry nonNullRegistry = Objects.requireNonNull(registry, "registry");
        nonNullRegistry.register(spec);
        return nonNullRegistry.obtain(spec.bind(
                MetricDimensionSet.of(dimension, dimensionValue),
                Objects.requireNonNull(source, "source")
        ));
    }

    static <S> FunctionCounter register(
            MetricRegistry registry,
            FunctionCounterSpec<S> spec,
            S source
    ) {
        MetricRegistry nonNullRegistry = Objects.requireNonNull(registry, "registry");
        nonNullRegistry.register(spec);
        return nonNullRegistry.obtain(spec.bind(MetricDimensionSet.empty(), Objects.requireNonNull(source, "source")));
    }

    static <S> FunctionCounter register(
            MetricRegistry registry,
            FunctionCounterSpec<S> spec,
            MetricDimensionSpec<String> dimension,
            String dimensionValue,
            S source
    ) {
        MetricRegistry nonNullRegistry = Objects.requireNonNull(registry, "registry");
        nonNullRegistry.register(spec);
        return nonNullRegistry.obtain(spec.bind(
                MetricDimensionSet.of(dimension, dimensionValue),
                Objects.requireNonNull(source, "source")
        ));
    }

    static <S> TimeGauge register(
            MetricRegistry registry,
            TimeGaugeSpec<S> spec,
            S source
    ) {
        MetricRegistry nonNullRegistry = Objects.requireNonNull(registry, "registry");
        nonNullRegistry.register(spec);
        return nonNullRegistry.obtain(spec.bind(MetricDimensionSet.empty(), Objects.requireNonNull(source, "source")));
    }
}
