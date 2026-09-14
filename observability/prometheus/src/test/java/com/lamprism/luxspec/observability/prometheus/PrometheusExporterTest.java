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

package com.lamprism.luxspec.observability.prometheus;

import com.lamprism.luxspec.observability.metric.CounterSpec;
import com.lamprism.luxspec.observability.metric.MetricDimensionSet;
import com.lamprism.luxspec.observability.metric.MetricDimensionSpec;
import com.lamprism.luxspec.observability.metric.MetricKind;
import com.lamprism.luxspec.observability.metric.MetricReading;
import com.lamprism.luxspec.observability.metric.MetricRegistry;
import com.lamprism.luxspec.observability.metric.MetricSnapshot;
import com.lamprism.luxspec.observability.metric.MetricSpec;
import com.lamprism.luxspec.observability.metric.TimeGaugeSpec;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrometheusExporterTest {
    @Test
    void groupsBindingsAndUsesOneSnapshotPerScrape() {
        MetricDimensionSpec<String> route = MetricDimensionSpec.required("route");
        CounterSpec spec = CounterSpec.builder("http.requests")
                .description("HTTP requests")
                .dimension(route)
                .build();
        MetricReading accounts = MetricReading.builder(
                        spec.bind(MetricDimensionSet.of(route, "/accounts")),
                        MetricKind.COUNTER
                )
                .value(3.0d)
                .build();
        MetricReading health = MetricReading.builder(
                        spec.bind(MetricDimensionSet.of(route, "/health")),
                        MetricKind.COUNTER
                )
                .value(1.0d)
                .build();
        SnapshotRegistry registry = new SnapshotRegistry(new MetricSnapshot(
                Instant.parse("2026-01-01T00:00:00Z"),
                1L,
                List.of(accounts, health)
        ));

        String output = new PrometheusExporter(registry).scrape();

        assertEquals(1, registry.snapshotCalls);
        assertEquals(1, occurrences(output, "# HELP http_requests"));
        assertEquals(1, occurrences(output, "# TYPE http_requests counter"));
        assertTrue(output.contains("http_requests{route=\"/accounts\"} 3.0"));
        assertTrue(output.contains("http_requests{route=\"/health\"} 1.0"));
    }

    @Test
    void rejectsDistinctSemanticNamesThatNormalizeToOneFamily() {
        CounterSpec first = CounterSpec.builder("cache-hits").build();
        CounterSpec second = CounterSpec.builder("cache_hits").build();
        MetricSnapshot snapshot = new MetricSnapshot(
                Instant.parse("2026-01-01T00:00:00Z"),
                1L,
                List.of(
                        MetricReading.builder(first.bind(), MetricKind.COUNTER).value(1.0d).build(),
                        MetricReading.builder(second.bind(), MetricKind.COUNTER).value(2.0d).build()
                )
        );

        assertThrows(PrometheusExportException.class, () -> new PrometheusExporter(new SnapshotRegistry(snapshot)).scrape());
    }

    @Test
    void rendersTimeGaugeAsOneGaugeSample() {
        TimeGaugeSpec<Object> spec = TimeGaugeSpec.builder("job.age", Object.class)
                .reader(value -> Duration.ofSeconds(2L))
                .build();
        MetricReading reading = MetricReading.builder(
                        spec.bind(MetricDimensionSet.empty(), new Object()),
                        MetricKind.TIME_GAUGE
                )
                .totalTime(Duration.ofSeconds(2L))
                .build();

        String output = new PrometheusExporter(new SnapshotRegistry(new MetricSnapshot(
                Instant.parse("2026-01-01T00:00:00Z"),
                1L,
                List.of(reading)
        ))).scrape();

        assertTrue(output.contains("job_age 2.0"));
        assertFalse(output.contains("job_age_sum"));
    }

    private static int occurrences(String text, String value) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(value, index)) >= 0) {
            count++;
            index += value.length();
        }
        return count;
    }

    private static final class SnapshotRegistry implements MetricRegistry {
        private final MetricSnapshot snapshot;
        private int snapshotCalls;

        private SnapshotRegistry(MetricSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public void register(MetricSpec<?> spec) {
            throw new UnsupportedOperationException();
        }

        @Override
        public MetricSpec<?> find(com.lamprism.luxspec.observability.metric.MetricName name) {
            return null;
        }

        @Override
        public <M extends com.lamprism.luxspec.observability.metric.Metric> M obtain(
                com.lamprism.luxspec.observability.metric.MetricBinding<M> binding
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <M extends com.lamprism.luxspec.observability.metric.Metric> M obtain(MetricSpec<M> spec) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <M extends com.lamprism.luxspec.observability.metric.Metric> M find(
                com.lamprism.luxspec.observability.metric.MetricBinding<M> binding
        ) {
            return null;
        }

        @Override
        public List<MetricSpec<?>> getSpecs() {
            return List.of();
        }

        @Override
        public MetricSnapshot snapshot() {
            snapshotCalls++;
            return snapshot;
        }

        @Override
        public void close() {
        }
    }
}
