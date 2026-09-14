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

package com.lamprism.luxspec.observability.metric.reducer;

import com.lamprism.luxspec.observability.metric.CounterSpec;
import com.lamprism.luxspec.observability.metric.MetricKind;
import com.lamprism.luxspec.observability.metric.MetricReading;
import com.lamprism.luxspec.observability.metric.MetricSample;
import com.lamprism.luxspec.observability.metric.TimerSpec;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MetricReducersTest {
    @Test
    void reducesAvailableScalarValuesAndIgnoresUnavailableValues() {
        CounterSpec spec = CounterSpec.builder("load").build();
        Instant start = Instant.parse("2026-01-01T00:00:00Z");

        MetricReducer<MetricValueSummary> reducer = new MetricSampleValueReducer();
        MetricValueSummary summary = reducer.reduce(
                List.of(
                        sample(spec, start.plusSeconds(2L), 3L, 3.0d),
                        sample(spec, start, 1L, 2.0d),
                        sample(spec, start.plusSeconds(1L), 2L, null)
                )
        );

        assertEquals(2, summary.getSampleCount());
        assertEquals(2.0d, summary.getFirstValue());
        assertEquals(3.0d, summary.getLastValue());
        assertEquals(2.0d, summary.getMinimum());
        assertEquals(3.0d, summary.getMaximum());
        assertEquals(5.0d, summary.getSum());
        assertEquals(2.5d, summary.getAverage());
    }

    @Test
    void reducesCounterDeltaRateAndResets() {
        CounterSpec spec = CounterSpec.builder("requests").build();
        Instant start = Instant.parse("2026-01-01T00:00:00Z");

        CounterSummary summary = new CounterSampleReducer().reduce(
                List.of(
                        sample(spec, start.plusSeconds(3L), 4L, 4.0d),
                        sample(spec, start, 1L, 5.0d),
                        sample(spec, start.plusSeconds(1L), 2L, 8.0d),
                        sample(spec, start.plusSeconds(2L), 3L, 2.0d)
                )
        );

        assertEquals(4, summary.getSampleCount());
        assertEquals(5.0d, summary.getFirstValue());
        assertEquals(4.0d, summary.getLastValue());
        assertEquals(7.0d, summary.getDelta());
        assertEquals(7.0d / 3.0d, summary.getRatePerSecond());
        assertEquals(1, summary.getResetCount());
    }

    @Test
    void reducesCumulativeHistogramAndExposesUpperBoundQuantiles() {
        TimerSpec spec = TimerSpec.builder("latency").build();
        Instant start = Instant.parse("2026-01-01T00:00:00Z");

        HistogramSummary summary = new HistogramSampleReducer().reduce(
                List.of(
                        histogramSample(spec, start, 1L, Map.of(1.0d, 2L, 2.0d, 3L), 3L),
                        histogramSample(spec, start.plusSeconds(1L), 2L, Map.of(1.0d, 3L, 2.0d, 5L), 5L)
                )
        );

        assertEquals(Map.of(1.0d, 1L, 2.0d, 2L), summary.getBuckets());
        assertEquals(2L, summary.getTotalCount());
        assertEquals(1.0d, summary.getQuantile(0.5d));
        assertEquals(2.0d, summary.getQuantile(0.95d));
        assertEquals(2.0d, summary.getQuantile(1.0d));
    }

    @Test
    void treatsCumulativeHistogramDecreasesAsResets() {
        TimerSpec spec = TimerSpec.builder("latency").build();
        Instant start = Instant.parse("2026-01-01T00:00:00Z");

        HistogramSummary summary = new HistogramSampleReducer().reduce(
                List.of(
                        histogramSample(spec, start, 1L, Map.of(1.0d, 2L, 2.0d, 4L), 4L),
                        histogramSample(spec, start.plusSeconds(1L), 2L, Map.of(1.0d, 1L, 2.0d, 2L), 2L)
                )
        );

        assertEquals(Map.of(1.0d, 1L, 2.0d, 2L), summary.getBuckets());
        assertEquals(2L, summary.getTotalCount());
    }

    @Test
    void reducesCumulativeCountsForTimerThroughput() {
        TimerSpec spec = TimerSpec.builder("requests").build();
        Instant start = Instant.parse("2026-01-01T00:00:00Z");

        CountSummary summary = new MetricSampleCountReducer().reduce(
                List.of(
                        histogramSample(spec, start.plusSeconds(2L), 3L, Map.of(), 2L),
                        histogramSample(spec, start, 1L, Map.of(), 5L),
                        histogramSample(spec, start.plusSeconds(1L), 2L, Map.of(), 8L)
                )
        );

        assertEquals(3, summary.getSampleCount());
        assertEquals(5L, summary.getFirstValue());
        assertEquals(2L, summary.getLastValue());
        assertEquals(5L, summary.getDelta());
        assertEquals(2.5d, summary.getRatePerSecond());
        assertEquals(1, summary.getResetCount());
    }

    @Test
    void rejectsMixedSeriesAndInvalidCounterValues() {
        CounterSpec firstSpec = CounterSpec.builder("first").build();
        CounterSpec secondSpec = CounterSpec.builder("second").build();
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");

        assertThrows(
                IllegalArgumentException.class,
                () -> new MetricSampleValueReducer().reduce(List.of(
                        sample(firstSpec, timestamp, 1L, 1.0d),
                        sample(secondSpec, timestamp, 1L, 2.0d)
                ))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new CounterSampleReducer().reduce(List.of(sample(firstSpec, timestamp, 1L, -1.0d)))
        );
    }

    private static MetricSample sample(
            CounterSpec spec,
            Instant timestamp,
            long sequence,
            Double value
    ) {
        MetricReading reading = MetricReading.builder(spec.bind(), MetricKind.COUNTER)
                .value(value)
                .build();
        return new MetricSample(timestamp, sequence, reading);
    }

    private static MetricSample histogramSample(
            TimerSpec spec,
            Instant timestamp,
            long sequence,
            Map<Double, Long> histogram,
            long count
    ) {
        MetricReading reading = MetricReading.builder(spec.bind(), MetricKind.TIMER)
                .count(count)
                .histogram(histogram)
                .build();
        return new MetricSample(timestamp, sequence, reading);
    }
}
