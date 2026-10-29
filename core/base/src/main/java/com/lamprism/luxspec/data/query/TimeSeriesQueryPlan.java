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

package com.lamprism.luxspec.data.query;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Query plan for timestamp bucketing and time-series reduction.
 *
 * <p>This type owns the timestamp, value, aggregation, bucket, and point-construction state for
 * a time-series operation. Projection-specific grouping and HAVING state belongs to
 * {@link ProjectionQueryPlan}.</p>
 *
 * @param <R> the final point result type
 *
 * @author RollW
 */
public final class TimeSeriesQueryPlan<R>
        implements QueryPlan<R> {
    private final QueryCriteria criteria;
    private final Class<?> valueType;
    private final QueryValue<Instant> timestamp;
    private final @Nullable QueryValue<?> value;
    private final TimeSeriesAggregationFunction aggregation;
    private final TimeSeriesBucket bucket;
    private final BiFunction<Instant, Object, R> pointFactory;

    private TimeSeriesQueryPlan(Builder<R> builder) {
        criteria = builder.criteria;
        valueType = builder.valueType;
        timestamp = builder.timestamp;
        value = builder.value;
        aggregation = builder.aggregation;
        bucket = builder.bucket;
        pointFactory = builder.pointFactory;
    }

    /**
     * Starts a standard time-series point plan.
     *
     * @param valueType   the reduced bucket value type
     * @param timestamp   the timestamp expression
     * @param value       the optional value expression
     * @param aggregation the bucket reduction function
     * @param bucket      the bucket definition
     * @param <V>         the reduced bucket value type
     * @return the time-series plan builder
     */
    public static <V> Builder<TimeSeriesPoint<V>> builder(
            Class<V> valueType,
            QueryValue<Instant> timestamp,
            @Nullable QueryValue<?> value,
            TimeSeriesAggregationFunction aggregation,
            TimeSeriesBucket bucket
    ) {
        Class<V> nonNullValueType = Objects.requireNonNull(valueType, "valueType");
        QueryValue<Instant> nonNullTimestamp = Objects.requireNonNull(timestamp, "timestamp");
        if (nonNullTimestamp.getValueType() != Instant.class) {
            throw new IllegalArgumentException("Time-series timestamp must be an Instant");
        }
        if (QueryValue.containsAggregate(nonNullTimestamp) || value != null && QueryValue.containsAggregate(value)) {
            throw new IllegalArgumentException("Time-series source expressions must not contain aggregates");
        }
        TimeSeriesAggregationFunction nonNullAggregation = Objects.requireNonNull(
                aggregation,
                "aggregation"
        );
        TimeSeriesBucket nonNullBucket = Objects.requireNonNull(bucket, "bucket");
        if (nonNullAggregation instanceof TimeSeriesAggregationFunction.Standard
                && nonNullAggregation != TimeSeriesAggregationFunction.COUNT && value == null) {
            throw new IllegalArgumentException("This aggregation requires a value expression");
        }
        return new Builder<>(
                nonNullValueType,
                nonNullTimestamp,
                value,
                nonNullAggregation,
                nonNullBucket,
                (pointTimestamp, bucketValue) -> new TimeSeriesPoint<>(
                        pointTimestamp,
                        nonNullValueType.cast(bucketValue)
                )
        );
    }

    /**
     * Returns the source criteria.
     *
     * @return the source criteria
     */
    @Override
    public QueryCriteria getCriteria() {
        return criteria;
    }

    /**
     * Returns the timestamp expression.
     *
     * @return the timestamp expression
     */
    public QueryValue<Instant> getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the optional value expression.
     *
     * @return the value expression, or null for row count
     */
    public @Nullable QueryValue<?> getValue() {
        return value;
    }

    /**
     * Returns the bucket reduction function.
     *
     * @return the aggregation function
     */
    public TimeSeriesAggregationFunction getAggregation() {
        return aggregation;
    }

    /**
     * Returns the bucket definition.
     *
     * @return the bucket definition
     */
    public TimeSeriesBucket getBucket() {
        return bucket;
    }

    /**
     * Returns the reduced bucket value type.
     *
     * @return the bucket value type
     */
    public Class<?> getValueType() {
        return valueType;
    }

    /**
     * Creates the final point result from one reduced bucket value.
     *
     * @param timestamp the bucket start
     * @param value     the reduced bucket value
     * @return the typed point result
     */
    public R point(Instant timestamp, Object value) {
        return pointFactory.apply(
                Objects.requireNonNull(timestamp, "timestamp"),
                Objects.requireNonNull(value, "value")
        );
    }

    /**
     * Builds an immutable time-series plan.
     *
     * @param <R> the final point result type
     */
    public static final class Builder<R> {
        private final Class<?> valueType;
        private final QueryValue<Instant> timestamp;
        private final @Nullable QueryValue<?> value;
        private final TimeSeriesAggregationFunction aggregation;
        private final TimeSeriesBucket bucket;
        private final BiFunction<Instant, Object, R> pointFactory;
        private QueryCriteria criteria = QueryCriteria.empty();

        private Builder(
                Class<?> valueType,
                QueryValue<Instant> timestamp,
                @Nullable QueryValue<?> value,
                TimeSeriesAggregationFunction aggregation,
                TimeSeriesBucket bucket,
                BiFunction<Instant, Object, R> pointFactory
        ) {
            this.valueType = valueType;
            this.timestamp = timestamp;
            this.value = value;
            this.aggregation = aggregation;
            this.bucket = bucket;
            this.pointFactory = pointFactory;
        }

        /**
         * Sets source filtering and ordering.
         *
         * @param criteria the source criteria
         * @return this builder
         */
        public Builder<R> criteria(QueryCriteria criteria) {
            this.criteria = Objects.requireNonNull(criteria, "criteria");
            return this;
        }

        /**
         * Creates the immutable time-series plan.
         *
         * @return the time-series plan
         */
        public TimeSeriesQueryPlan<R> build() {
            return new TimeSeriesQueryPlan<>(this);
        }
    }
}
