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
import com.lamprism.luxspec.data.pagination.QueryWindow;
import com.lamprism.luxspec.data.query.InMemoryQueryExecutor;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.data.query.QueryExecutor;
import com.lamprism.luxspec.observability.metric.MetricName;
import com.lamprism.luxspec.observability.metric.MetricReading;
import com.lamprism.luxspec.observability.metric.MetricSample;
import com.lamprism.luxspec.observability.metric.MetricSnapshot;
import com.lamprism.luxspec.observability.metric.MetricSnapshotSink;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Keeps a bounded in-memory history of complete metric snapshots.
 *
 * <p>The store retains snapshots rather than individual readings, so every capture remains an
 * atomic set of metric readings. Queries flatten a stable copy of the retained snapshots into
 * {@link MetricSample} items and use the regular {@link QueryExecutor} contract. The query does not
 * hold the store lock while filtering, ordering, or windowing.</p>
 *
 * <p>The bound applies to snapshots. The memory used by each snapshot still depends on the number
 * of materialized metric bindings in that snapshot.</p>
 *
 * @author RollW
 */
public final class InMemoryMetricSnapshotStore implements MetricSnapshotSink, QueryExecutor<MetricSample> {
    private final Object monitor = new Object();
    private final int maximumSnapshots;
    private final Deque<MetricSnapshot> snapshots = new ArrayDeque<>();
    private final InMemoryQueryExecutor<MetricSample> queryExecutor;

    /**
     * Creates a count-bounded snapshot store.
     *
     * @param maximumSnapshots the maximum number of retained snapshots
     */
    public InMemoryMetricSnapshotStore(int maximumSnapshots) {
        if (maximumSnapshots < 1) {
            throw new IllegalArgumentException("maximumSnapshots must be positive");
        }
        this.maximumSnapshots = maximumSnapshots;
        this.queryExecutor = createQueryExecutor();
    }

    /**
     * Retains one immutable metric snapshot.
     *
     * <p>When the bound is exceeded, the oldest retained snapshot is removed.</p>
     *
     * @param snapshot the snapshot to retain
     */
    @Override
    public void accept(MetricSnapshot snapshot) {
        MetricSnapshot nonNullSnapshot = Objects.requireNonNull(snapshot, "snapshot");
        synchronized (monitor) {
            snapshots.addLast(nonNullSnapshot);
            while (snapshots.size() > maximumSnapshots) {
                snapshots.removeFirst();
            }
        }
    }

    /**
     * Executes a regular or time-series query over retained metric samples.
     *
     * <p>The caller owns validation with the role-specific {@code QuerySchema} and
     * {@code QueryComplexityBudget}.</p>
     *
     * @param criteria the structured filters and ordering
     * @param window   the requested result window
     * @return the complete, page, or slice result
     */
    @Override
    public QueryResult<MetricSample> query(QueryCriteria criteria, QueryWindow window) {
        return queryExecutor.query(criteria, window);
    }

    /**
     * Returns the configured snapshot bound.
     *
     * @return the maximum number of retained snapshots
     */
    public int getMaximumSnapshots() {
        return maximumSnapshots;
    }

    /**
     * Returns the current number of retained snapshots.
     *
     * @return the retained snapshot count
     */
    public int getSnapshotCount() {
        synchronized (monitor) {
            return snapshots.size();
        }
    }

    /**
     * Removes all retained snapshots.
     */
    public void clear() {
        synchronized (monitor) {
            snapshots.clear();
        }
    }

    private InMemoryQueryExecutor<MetricSample> createQueryExecutor() {
        return InMemoryQueryExecutor.<MetricSample>builder(this::samples)
                .field(MetricSample.TIMESTAMP, MetricSample::getTimestamp)
                .field(MetricSample.SEQUENCE, MetricSample::getSequence)
                .field(
                        MetricSample.METRIC_NAME,
                        MetricSample::getMetricName,
                        Comparator.comparing(MetricName::getValue)
                )
                .field(MetricSample.METRIC_KIND, MetricSample::getMetricKind)
                .field(MetricSample.DIMENSIONS, MetricSample::getDimensions)
                .field(MetricSample.HISTOGRAM, MetricSample::getHistogram)
                .field(MetricSample.VALUE, MetricSample::getValue)
                .field(MetricSample.COUNT, MetricSample::getCount)
                .field(MetricSample.TOTAL, MetricSample::getTotal)
                .field(MetricSample.MAX, MetricSample::getMax)
                .field(MetricSample.TOTAL_TIME, MetricSample::getTotalTime)
                .field(MetricSample.ACTIVE_DURATION, MetricSample::getActiveDuration)
                .field(MetricSample.ACTIVE_TASKS, MetricSample::getActiveTasks)
                .build();
    }

    private Collection<? extends MetricSample> samples() {
        List<MetricSnapshot> retainedSnapshots;
        synchronized (monitor) {
            retainedSnapshots = List.copyOf(snapshots);
        }

        List<MetricSample> samples = new ArrayList<>();
        for (MetricSnapshot snapshot : retainedSnapshots) {
            for (MetricReading reading : snapshot.getReadings()) {
                samples.add(new MetricSample(snapshot.getCapturedAt(), snapshot.getSequence(), reading));
            }
        }
        return samples;
    }
}
