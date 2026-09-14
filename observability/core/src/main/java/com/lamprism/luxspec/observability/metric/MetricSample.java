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

import com.lamprism.luxspec.data.query.QueryField;
import com.lamprism.luxspec.data.query.TimeSeriesPoint;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * One timestamped reading flattened from a {@link MetricSnapshot}.
 *
 * <p>A sample keeps the snapshot sequence so a dashboard adapter can correlate samples captured
 * in the same registry pass. Reading values remain nullable because not every metric kind
 * exposes every value shape.</p>
 *
 * @author RollW
 */
public final class MetricSample {
    /**
     * The snapshot capture timestamp.
     */
    public static final QueryField<Instant> TIMESTAMP = TimeSeriesPoint.TIMESTAMP;
    /**
     * The source snapshot sequence.
     */
    public static final QueryField<Long> SEQUENCE = QueryField.of("sequence", Long.class);
    /**
     * The semantic metric name.
     */
    public static final QueryField<MetricName> METRIC_NAME = QueryField.of("metricName", MetricName.class);
    /**
     * The provider-neutral metric kind value.
     */
    public static final QueryField<String> METRIC_KIND = QueryField.of("metricKind", String.class);
    /**
     * The complete formatted dimension map.
     */
    public static final QueryField<Map<String, String>> DIMENSIONS = dimensionsField();
    /**
     * The complete cumulative histogram bucket map when available.
     */
    public static final QueryField<Map<Double, Long>> HISTOGRAM = histogramField();
    /**
     * The scalar reading value when the metric exposes one.
     */
    public static final QueryField<Double> VALUE = QueryField.of("value", Double.class);
    /**
     * The cumulative or observed reading count when available.
     */
    public static final QueryField<Long> COUNT = QueryField.of("count", Long.class);
    /**
     * The cumulative reading total when available.
     */
    public static final QueryField<Double> TOTAL = QueryField.of("total", Double.class);
    /**
     * The maximum reading when available.
     */
    public static final QueryField<Double> MAX = QueryField.of("max", Double.class);
    /**
     * The cumulative duration when available.
     */
    public static final QueryField<Duration> TOTAL_TIME = QueryField.of("totalTime", Duration.class);
    /**
     * The active duration when available.
     */
    public static final QueryField<Duration> ACTIVE_DURATION = QueryField.of("activeDuration", Duration.class);
    /**
     * The active task count when available.
     */
    public static final QueryField<Long> ACTIVE_TASKS = QueryField.of("activeTasks", Long.class);

    private static final Set<QueryField<?>> COMMON_FIELDS = Set.of(
            TIMESTAMP,
            SEQUENCE,
            METRIC_NAME,
            METRIC_KIND,
            DIMENSIONS
    );
    private static final Set<QueryField<?>> ALL_FIELDS = fields(
            VALUE,
            COUNT,
            TOTAL,
            MAX,
            TOTAL_TIME,
            ACTIVE_DURATION,
            ACTIVE_TASKS,
            HISTOGRAM
    );

    private final Instant timestamp;
    private final long sequence;
    private final MetricReading reading;
    private final MetricSeriesKey seriesKey;

    /**
     * Creates one immutable metric sample.
     *
     * @param timestamp the snapshot capture time
     * @param sequence  the snapshot sequence
     * @param reading   the captured metric reading
     */
    public MetricSample(Instant timestamp, long sequence, MetricReading reading) {
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        if (sequence < 0L) {
            throw new IllegalArgumentException("sequence must not be negative");
        }
        this.sequence = sequence;
        this.reading = Objects.requireNonNull(reading, "reading");
        this.seriesKey = new MetricSeriesKey(
                reading.getBinding().getSpec().getName(),
                reading.getBinding().getDimensions()
        );
    }

    /**
     * Returns the capture timestamp used by time-series queries.
     *
     * @return the sample timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the source snapshot sequence.
     *
     * @return the snapshot sequence
     */
    public long getSequence() {
        return sequence;
    }

    /**
     * Returns the underlying immutable metric reading.
     *
     * @return the metric reading
     */
    public MetricReading getReading() {
        return reading;
    }

    /**
     * Returns the stable metric series identity.
     *
     * @return the metric name and complete formatted dimensions
     */
    public MetricSeriesKey getSeriesKey() {
        return seriesKey;
    }

    /**
     * Returns all standard fields exposed by metric samples.
     *
     * @return immutable standard fields
     */
    public static Set<QueryField<?>> fields() {
        Set<QueryField<?>> result = new LinkedHashSet<>(COMMON_FIELDS);
        result.addAll(ALL_FIELDS);
        return Set.copyOf(result);
    }

    /**
     * Returns the standard fields supported by this sample's metric kind.
     *
     * <p>A supported field may still be unavailable on this particular reading and therefore
     * resolve to null.</p>
     *
     * @return immutable fields supported by the metric kind
     */
    public Set<QueryField<?>> getAvailableFields() {
        return fieldsFor(reading.getKind());
    }

    /**
     * Returns the fields structurally supported by one built-in metric kind.
     *
     * @param kind the metric kind
     * @return immutable fields supported by the kind
     */
    public static Set<QueryField<?>> fieldsFor(MetricKind<?> kind) {
        MetricKind<?> nonNullKind = Objects.requireNonNull(kind, "kind");
        if (MetricKind.COUNTER.equals(nonNullKind)
                || MetricKind.FUNCTION_COUNTER.equals(nonNullKind)
                || MetricKind.GAUGE.equals(nonNullKind)) {
            return fields(VALUE);
        }
        if (MetricKind.TIMER.equals(nonNullKind)) {
            return fields(COUNT, MAX, TOTAL_TIME, HISTOGRAM);
        }
        if (MetricKind.DISTRIBUTION_SUMMARY.equals(nonNullKind)) {
            return fields(COUNT, TOTAL, MAX, HISTOGRAM);
        }
        if (MetricKind.LONG_TASK_TIMER.equals(nonNullKind)) {
            return fields(ACTIVE_DURATION, ACTIVE_TASKS);
        }
        if (MetricKind.FUNCTION_TIMER.equals(nonNullKind)) {
            return fields(COUNT, TOTAL_TIME);
        }
        if (MetricKind.TIME_GAUGE.equals(nonNullKind)) {
            return fields(TOTAL_TIME);
        }
        throw new IllegalArgumentException("Unsupported metric kind: " + nonNullKind.value());
    }

    /**
     * Returns the semantic metric name.
     *
     * @return the metric name
     */
    public MetricName getMetricName() {
        return seriesKey.getMetricName();
    }

    /**
     * Returns the provider-neutral metric kind value.
     *
     * @return the metric kind value
     */
    public String getMetricKind() {
        return reading.getKind().value();
    }

    /**
     * Returns the formatted metric dimensions.
     *
     * @return immutable metric dimensions
     */
    public Map<String, String> getDimensions() {
        return seriesKey.getDimensions();
    }

    public @Nullable Double getValue() {
        return reading.getValue();
    }

    public @Nullable Long getCount() {
        return reading.getCount();
    }

    public @Nullable Double getTotal() {
        return reading.getTotal();
    }

    public @Nullable Double getMax() {
        return reading.getMax();
    }

    public @Nullable Duration getTotalTime() {
        return reading.getTotalTime();
    }

    public @Nullable Duration getActiveDuration() {
        return reading.getActiveDuration();
    }

    public @Nullable Long getActiveTasks() {
        return reading.getActiveTasks();
    }

    /**
     * Returns cumulative histogram buckets when the reading provides them.
     *
     * @return immutable bucket upper bounds and cumulative counts
     */
    public Map<Double, Long> getHistogram() {
        return reading.histogram();
    }

    private static Set<QueryField<?>> fields(QueryField<?>... specificFields) {
        Set<QueryField<?>> result = new LinkedHashSet<>(COMMON_FIELDS);
        Collections.addAll(result, specificFields);
        return Set.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static QueryField<Map<String, String>> dimensionsField() {
        return (QueryField<Map<String, String>>) (QueryField<?>) QueryField.of("dimensions", Map.class);
    }

    @SuppressWarnings("unchecked")
    private static QueryField<Map<Double, Long>> histogramField() {
        return (QueryField<Map<Double, Long>>) (QueryField<?>) QueryField.of("histogram", Map.class);
    }
}
