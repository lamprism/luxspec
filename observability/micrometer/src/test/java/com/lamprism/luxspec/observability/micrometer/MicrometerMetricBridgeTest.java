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

package com.lamprism.luxspec.observability.micrometer;

import com.lamprism.luxspec.observability.metric.Counter;
import com.lamprism.luxspec.observability.metric.CounterSpec;
import com.lamprism.luxspec.observability.metric.LongTaskTimer;
import com.lamprism.luxspec.observability.metric.LongTaskTimerSpec;
import com.lamprism.luxspec.observability.metric.MetricRegistry;
import com.lamprism.luxspec.observability.runtime.metric.MetricRegistryBuilder;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicrometerMetricBridgeTest {
    @Test
    void projectsMaterializedCounterAndLeavesProviderMeterOwnedByApplication() {
        CounterSpec spec = CounterSpec.builder("http.requests").build();

        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        try (MetricRegistry registry = MetricRegistryBuilder.builder().build()) {
            registry.register(spec);
            try (MicrometerMetricBridge ignored = new MicrometerMetricBridge(registry, meterRegistry)) {
                Counter counter = registry.obtain(spec);
                counter.add(2.5d);

                assertEquals(2.5d, meterRegistry.get("http.requests").functionCounter().count());
                assertEquals(1, meterRegistry.getMeters().size());
            }

            assertEquals(1, meterRegistry.getMeters().size());
        }
    }

    @Test
    void projectsLongTaskMeasurementsWithProviderType() {
        LongTaskTimerSpec spec = LongTaskTimerSpec.builder("jobs.active").build();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        try (MetricRegistry registry = MetricRegistryBuilder.builder().build();
             MicrometerMetricBridge ignored = new MicrometerMetricBridge(registry, meterRegistry)) {
            registry.register(spec);
            LongTaskTimer timer = registry.obtain(spec);
            try (var timing = timer.start()) {
                Meter meter = meterRegistry.get("jobs.active").meter();
                assertEquals(Meter.Type.LONG_TASK_TIMER, meter.getId().getType());
                assertTrue(measurementValue(meter, Statistic.ACTIVE_TASKS) >= 1.0d);
                assertTrue(measurementValue(meter, Statistic.DURATION) >= 0.0d);
            }
        }
    }

    private static double measurementValue(Meter meter, Statistic statistic) {
        for (Measurement measurement : meter.measure()) {
            if (measurement.getStatistic() == statistic) {
                return measurement.getValue();
            }
        }
        throw new AssertionError("Missing measurement: " + statistic);
    }
}
