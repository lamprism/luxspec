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

import com.lamprism.luxspec.data.query.ProjectionQueryPlan.Builder;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Unified provider-neutral query plan contract.
 *
 * <p>Plan types own the invariants of their operation shape. Applications may define additional
 * plan types for their own executors and validation; standard executors support projection and
 * time-series plans.</p>
 *
 * @param <R> the result item type
 *
 * @author RollW
 */
public interface QueryPlan<R> {

    /**
     * Returns the source criteria shared by all plan shapes.
     *
     * @return the source criteria
     */
    QueryCriteria getCriteria();

    /**
     * Starts a projection plan.
     *
     * @param projection the typed projection
     * @param <R>        the projected result type
     * @return the projection plan builder
     */
    static <R> Builder<R> builder(QueryProjection<R> projection) {
        return ProjectionQueryPlan.builder(projection);
    }

    /**
     * Starts a plan that projects one typed value.
     *
     * @param value the selected value
     * @param <R>   the projected value type
     * @return the projection plan builder
     */
    static <R> Builder<R> builder(QueryValue<R> value) {
        return builder(QueryProjection.single(value));
    }

    /**
     * Starts a count plan over rows in each time bucket.
     *
     * @param timestamp the timestamp expression
     * @param bucket    the bucket definition
     * @return the time-series plan builder
     */
    static TimeSeriesQueryPlan.Builder<TimeSeriesPoint<Long>> count(
            QueryValue<Instant> timestamp,
            TimeSeriesBucket bucket
    ) {
        return timeSeries(Long.class, timestamp, null, TimeSeriesAggregationFunction.COUNT, bucket);
    }

    /**
     * Starts a count-distinct plan over values in each time bucket.
     *
     * @param timestamp the timestamp expression
     * @param value     the counted value expression
     * @param bucket    the bucket definition
     * @param <T>       the counted value type
     * @return the time-series plan builder
     */
    static <T> TimeSeriesQueryPlan.Builder<TimeSeriesPoint<Long>> countDistinct(
            QueryValue<Instant> timestamp,
            QueryValue<T> value,
            TimeSeriesBucket bucket
    ) {
        return timeSeries(
                Long.class,
                timestamp,
                value,
                TimeSeriesAggregationFunction.COUNT_DISTINCT,
                bucket
        );
    }

    /**
     * Starts a numeric sum plan over values in each time bucket.
     *
     * @param timestamp the timestamp expression
     * @param value     the numeric value expression
     * @param bucket    the bucket definition
     * @param <T>       the numeric value type
     * @return the time-series plan builder
     */
    static <T extends Number> TimeSeriesQueryPlan.Builder<TimeSeriesPoint<BigDecimal>> sum(
            QueryValue<Instant> timestamp,
            QueryValue<T> value,
            TimeSeriesBucket bucket
    ) {
        return timeSeries(
                BigDecimal.class,
                timestamp,
                value,
                TimeSeriesAggregationFunction.SUM,
                bucket
        );
    }

    /**
     * Starts an average plan over values in each time bucket.
     *
     * @param timestamp the timestamp expression
     * @param value     the numeric value expression
     * @param bucket    the bucket definition
     * @param <T>       the numeric value type
     * @return the time-series plan builder
     */
    static <T extends Number> TimeSeriesQueryPlan.Builder<TimeSeriesPoint<Double>> average(
            QueryValue<Instant> timestamp,
            QueryValue<T> value,
            TimeSeriesBucket bucket
    ) {
        return timeSeries(
                Double.class,
                timestamp,
                value,
                TimeSeriesAggregationFunction.AVERAGE,
                bucket
        );
    }

    /**
     * Starts a minimum plan over values in each time bucket.
     *
     * @param timestamp the timestamp expression
     * @param value     the comparable value expression
     * @param bucket    the bucket definition
     * @param <T>       the value type
     * @return the time-series plan builder
     */
    static <T extends Comparable<? super T>> TimeSeriesQueryPlan.Builder<TimeSeriesPoint<T>> minimum(
            QueryValue<Instant> timestamp,
            QueryValue<T> value,
            TimeSeriesBucket bucket
    ) {
        return timeSeries(
                value.getValueType(),
                timestamp,
                value,
                TimeSeriesAggregationFunction.MIN,
                bucket
        );
    }

    /**
     * Starts a maximum plan over values in each time bucket.
     *
     * @param timestamp the timestamp expression
     * @param value     the comparable value expression
     * @param bucket    the bucket definition
     * @param <T>       the value type
     * @return the time-series plan builder
     */
    static <T extends Comparable<? super T>> TimeSeriesQueryPlan.Builder<TimeSeriesPoint<T>> maximum(
            QueryValue<Instant> timestamp,
            QueryValue<T> value,
            TimeSeriesBucket bucket
    ) {
        return timeSeries(
                value.getValueType(),
                timestamp,
                value,
                TimeSeriesAggregationFunction.MAX,
                bucket
        );
    }

    /**
     * Starts a time-series plan with an explicit reduction function.
     *
     * @param valueType   the reduced bucket value type
     * @param timestamp   the timestamp expression
     * @param value       the optional value expression
     * @param aggregation the bucket reduction function
     * @param bucket      the bucket definition
     * @param <V>         the reduced bucket value type
     * @return the time-series plan builder
     */
    static <V> TimeSeriesQueryPlan.Builder<TimeSeriesPoint<V>> timeSeries(
            Class<V> valueType,
            QueryValue<Instant> timestamp,
            @Nullable QueryValue<?> value,
            TimeSeriesAggregationFunction aggregation,
            TimeSeriesBucket bucket
    ) {
        return TimeSeriesQueryPlan.builder(valueType, timestamp, value, aggregation, bucket);
    }
}
