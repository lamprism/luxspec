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

package com.lamprism.luxspec.observability.runtime.history;

import com.lamprism.luxspec.data.pagination.QueryResult;
import com.lamprism.luxspec.data.pagination.SliceResult;
import com.lamprism.luxspec.data.pagination.SliceWindow;
import com.lamprism.luxspec.data.pagination.TimeSeriesWindow;
import com.lamprism.luxspec.data.pagination.UnboundedWindow;
import com.lamprism.luxspec.data.query.QueryComplexityBudget;
import com.lamprism.luxspec.data.query.QueryCondition;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.data.query.QueryOperator;
import com.lamprism.luxspec.data.query.QuerySchema;
import com.lamprism.luxspec.observability.metric.CounterSpec;
import com.lamprism.luxspec.observability.metric.MetricDimensionSet;
import com.lamprism.luxspec.observability.metric.MetricDimensionSpec;
import com.lamprism.luxspec.observability.metric.MetricKind;
import com.lamprism.luxspec.observability.metric.MetricName;
import com.lamprism.luxspec.observability.metric.MetricReading;
import com.lamprism.luxspec.observability.metric.MetricSample;
import com.lamprism.luxspec.observability.metric.MetricSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryMetricSnapshotStoreTest {
    @Test
    void retainsCompleteSnapshotsAndQueriesFlattenedSamples() {
        Instant first = Instant.parse("2026-01-01T00:00:00Z");
        InMemoryMetricSnapshotStore store = new InMemoryMetricSnapshotStore(2);
        store.accept(snapshot(first, 1L, "requests", 1.0d));
        store.accept(snapshot(first.plusSeconds(1L), 2L, "requests", 2.0d));
        store.accept(snapshot(first.plusSeconds(2L), 3L, "requests", 3.0d));

        QueryCriteria criteria = new QueryCriteria(
                QueryCondition.equal(MetricSample.METRIC_NAME, MetricName.of("requests")),
                List.of()
        );
        QuerySchema schema = QuerySchema.builder()
                .add(MetricSample.METRIC_NAME, Set.of(QueryOperator.EQUAL))
                .build();
        schema.validate(criteria, QueryComplexityBudget.defaults());

        QueryResult<MetricSample> queryResult = store.query(
                criteria,
                new TimeSeriesWindow(first, first.plusSeconds(2L), 10)
        );
        SliceResult<MetricSample> result = assertInstanceOf(SliceResult.class, queryResult);

        assertEquals(2, store.getSnapshotCount());
        assertEquals(
                List.of(2.0d, 3.0d),
                result.getItems().stream().map(MetricSample::getValue).toList()
        );
        assertEquals(
                List.of(first.plusSeconds(1L), first.plusSeconds(2L)),
                result.getItems().stream().map(MetricSample::getTimestamp).toList()
        );
    }

    @Test
    void supportsRegularQueryWindowsAndClear() {
        Instant first = Instant.parse("2026-01-01T00:00:00Z");
        InMemoryMetricSnapshotStore store = new InMemoryMetricSnapshotStore(3);
        store.accept(snapshot(first, 1L, "requests", 1.0d));
        store.accept(snapshot(first.plusSeconds(1L), 2L, "requests", 2.0d));

        SliceResult<MetricSample> result = assertInstanceOf(
                SliceResult.class,
                store.query(
                        QueryCriteria.empty(),
                        new SliceWindow(1L, 1)
                )
        );

        assertEquals(2.0d, result.getItems().get(0).getValue());
        assertEquals(2, store.getSnapshotCount());
        store.clear();
        assertEquals(0, store.getSnapshotCount());
        assertEquals(
                List.of(),
                store.query(
                        QueryCriteria.empty(),
                        UnboundedWindow.getInstance()
                ).getItems()
        );
    }

    @Test
    void filtersByTheCompleteFormattedDimensionMap() {
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
        MetricDimensionSpec<String> route = MetricDimensionSpec.required("route");
        CounterSpec spec = CounterSpec.builder("requests")
                .dimension(route)
                .build();
        MetricReading reading = MetricReading.builder(
                        spec.bind(MetricDimensionSet.of(route, "/accounts")),
                        MetricKind.COUNTER
                )
                .value(1.0d)
                .build();
        InMemoryMetricSnapshotStore store = new InMemoryMetricSnapshotStore(2);
        store.accept(new MetricSnapshot(timestamp, 1L, List.of(reading)));

        QueryCriteria criteria = new QueryCriteria(
                QueryCondition.equal(
                        MetricSample.DIMENSIONS,
                        Map.of("route", "/accounts")
                ),
                List.of()
        );
        QuerySchema.builder()
                .add(MetricSample.DIMENSIONS, Set.of(QueryOperator.EQUAL))
                .build()
                .validate(criteria, QueryComplexityBudget.defaults());

        QueryResult<MetricSample> result = store.query(
                criteria,
                new TimeSeriesWindow(timestamp, timestamp, 1)
        );

        assertEquals(1, result.getItems().size());
        assertEquals(Map.of("route", "/accounts"), result.getItems().get(0).getDimensions());
    }

    @Test
    void rejectsAnInvalidSnapshotBound() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InMemoryMetricSnapshotStore(0)
        );
    }

    private static MetricSnapshot snapshot(Instant timestamp, long sequence, String name, double value) {
        CounterSpec spec = CounterSpec.builder(name).build();
        MetricReading reading = MetricReading.builder(
                        spec.bind(),
                        MetricKind.COUNTER
                )
                .value(value)
                .build();
        return new MetricSnapshot(timestamp, sequence, List.of(reading));
    }
}
