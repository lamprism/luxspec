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

import com.lamprism.luxspec.observability.ObservabilityClock;
import com.lamprism.luxspec.observability.SystemObservabilityClock;
import com.lamprism.luxspec.observability.metric.MetricActivation;
import com.lamprism.luxspec.observability.metric.MetricRegistry;
import com.lamprism.luxspec.observability.runtime.ObservabilitySet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds the default in-process metric registry.
 *
 * @author RollW
 */
public final class MetricRegistryBuilder {
    private ObservabilityClock clock = new SystemObservabilityClock();
    private MetricActivation activation = MetricActivation.all();
    private final List<ObservabilitySet> sets = new ArrayList<>();

    private MetricRegistryBuilder() {
    }

    public static MetricRegistryBuilder builder() {
        return new MetricRegistryBuilder();
    }

    public MetricRegistryBuilder clock(ObservabilityClock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
        return this;
    }

    public MetricRegistryBuilder activation(MetricActivation activation) {
        this.activation = Objects.requireNonNull(activation, "activation");
        return this;
    }

    /**
     * Selects one domain contribution for this registry.
     *
     * @param set the selected observability set
     * @return this builder
     */
    public MetricRegistryBuilder set(ObservabilitySet set) {
        sets.add(Objects.requireNonNull(set, "set"));
        return this;
    }

    public MetricRegistry build() {
        MetricRegistry registry = new DefaultMetricRegistry(clock, activation);
        try {
            for (ObservabilitySet set : sets) {
                set.registerMetrics(registry);
            }
            return registry;
        } catch (RuntimeException | Error failure) {
            registry.close();
            throw failure;
        }
    }
}
