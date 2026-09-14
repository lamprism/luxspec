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

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricSeriesTest {
    @Test
    void groupsByMetricNameAndCompleteDimensionsAndLimitsEachSeries() {
        MetricDimensionSpec<String> route = MetricDimensionSpec.required("route");
        CounterSpec spec = CounterSpec.builder("requests")
                .dimension(route)
                .build();
        Instant start = Instant.parse("2026-01-01T00:00:00Z");

        List<MetricSeries> series = MetricSeries.group(
                List.of(
                        sample(spec, route, "/accounts", start.plusSeconds(2L), 3L, 30.0d),
                        sample(spec, route, "/health", start.plusSeconds(1L), 2L, 20.0d),
                        sample(spec, route, "/accounts", start, 1L, 10.0d),
                        sample(spec, route, "/accounts", start.plusSeconds(3L), 4L, 40.0d)
                ),
                2
        );

        assertEquals(2, series.size());
        assertEquals(Map.of("route", "/accounts"), series.get(0).getKey().getDimensions());
        assertEquals(
                List.of(10.0d, 30.0d),
                series.get(0).getSamples().stream().map(MetricSample::getValue).toList()
        );
        assertEquals(Map.of("route", "/health"), series.get(1).getKey().getDimensions());
        assertEquals(List.of(20.0d), series.get(1).getSamples().stream().map(MetricSample::getValue).toList());
    }

    @Test
    void copiesSeriesKeyDimensionsAndRejectsInvalidPointLimit() {
        Map<String, String> dimensions = new LinkedHashMap<>();
        dimensions.put("route", "/accounts");
        MetricSeriesKey key = new MetricSeriesKey(MetricName.of("requests"), dimensions);
        dimensions.put("route", "/changed");

        assertEquals(Map.of("route", "/accounts"), key.getDimensions());
        assertEquals(
                key,
                new MetricSeriesKey(MetricName.of("requests"), Map.of("route", "/accounts"))
        );
        assertThrows(UnsupportedOperationException.class, () -> key.getDimensions().put("new", "value"));
        assertThrows(IllegalArgumentException.class, () -> MetricSeries.group(List.of(), 0));
    }

    @Test
    void exposesStandardAndMetricKindSpecificFields() {
        MetricDimensionSpec<String> route = MetricDimensionSpec.required("route");
        CounterSpec counter = CounterSpec.builder("requests")
                .dimension(route)
                .build();
        MetricSample sample = sample(
                counter,
                route,
                "/accounts",
                Instant.parse("2026-01-01T00:00:00Z"),
                1L,
                1.0d
        );

        assertTrue(MetricSample.fields().containsAll(Set.of(
                MetricSample.TIMESTAMP,
                MetricSample.HISTOGRAM
        )));
        assertTrue(sample.getAvailableFields().contains(MetricSample.VALUE));
        assertFalse(sample.getAvailableFields().contains(MetricSample.COUNT));
        assertTrue(MetricSample.fieldsFor(MetricKind.TIMER).contains(MetricSample.HISTOGRAM));
        assertFalse(MetricSample.fieldsFor(MetricKind.TIMER).contains(MetricSample.VALUE));
    }

    private static MetricSample sample(
            CounterSpec spec,
            MetricDimensionSpec<String> route,
            String routeValue,
            Instant timestamp,
            long sequence,
            double value
    ) {
        MetricReading reading = MetricReading.builder(
                        spec.bind(MetricDimensionSet.of(route, routeValue)),
                        MetricKind.COUNTER
                )
                .value(value)
                .build();
        return new MetricSample(timestamp, sequence, reading);
    }
}
