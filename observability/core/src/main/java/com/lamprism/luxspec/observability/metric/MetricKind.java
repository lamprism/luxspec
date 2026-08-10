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

package com.lamprism.luxspec.observability.metric;

import java.util.Objects;

/**
 * Stable provider-neutral metric instrument kind.
 *
 * <p>The initial Core API exposes a closed set of built-in kinds. The native
 * runtime materializes the corresponding specialized spec types; declaring a
 * new kind requires an explicit runtime and adapter extension.
 *
 * @author RollW
 */
public final class MetricKind<M extends Metric> {
    public static final MetricKind<Counter> COUNTER = new MetricKind<>("counter");
    public static final MetricKind<Gauge> GAUGE = new MetricKind<>("gauge");
    public static final MetricKind<FunctionCounter> FUNCTION_COUNTER = new MetricKind<>("function_counter");
    public static final MetricKind<Timer> TIMER = new MetricKind<>("timer");
    public static final MetricKind<DistributionSummary> DISTRIBUTION_SUMMARY = new MetricKind<>("distribution_summary");
    public static final MetricKind<LongTaskTimer> LONG_TASK_TIMER = new MetricKind<>("long_task_timer");
    public static final MetricKind<FunctionTimer> FUNCTION_TIMER = new MetricKind<>("function_timer");
    public static final MetricKind<TimeGauge> TIME_GAUGE = new MetricKind<>("time_gauge");

    private final String value;

    private MetricKind(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MetricKind<?> kind)) {
            return false;
        }
        return value.equals(kind.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
