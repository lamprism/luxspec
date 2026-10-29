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

import java.util.List;

/**
 * Reduces the source values in one time-series bucket.
 *
 * <p>Implementations must be safe for concurrent use when plans are shared. Their results must
 * match the result type declared by the plan; returning null omits the bucket. Equality and
 * hash codes must remain stable while an implementation is registered in a schema.</p>
 *
 * @author RollW
 */
@FunctionalInterface
public interface TimeSeriesAggregationFunction {
    /**
     * Counts rows or non-null values in each bucket.
     */
    Standard COUNT = Standard.COUNT;
    /**
     * Counts distinct non-null values in each bucket.
     */
    Standard COUNT_DISTINCT = Standard.COUNT_DISTINCT;
    /**
     * Sums non-null numeric values in each bucket.
     */
    Standard SUM = Standard.SUM;
    /**
     * Averages non-null numeric values in each bucket.
     */
    Standard AVERAGE = Standard.AVERAGE;
    /**
     * Selects the lowest non-null value in each bucket.
     */
    Standard MIN = Standard.MIN;
    /**
     * Selects the highest non-null value in each bucket.
     */
    Standard MAX = Standard.MAX;

    /**
     * Reduces one bucket's values, including nulls.
     *
     * @param values   immutable values in ascending timestamp order; rows with the same timestamp
     *                 have unspecified relative order, and a row-only plan supplies one null per row
     * @param hasValue whether the plan selected a value expression
     * @return the reduced value, or null to omit the bucket
     */
    @Nullable Object reduce(List<@Nullable Object> values, boolean hasValue);

    /**
     * Standard time-series reductions.
     */
    enum Standard implements TimeSeriesAggregationFunction {
        /**
         * Counts rows or non-null values.
         */
        COUNT,
        /**
         * Counts distinct non-null values.
         */
        COUNT_DISTINCT,
        /**
         * Sums non-null numeric values.
         */
        SUM,
        /**
         * Averages non-null numeric values.
         */
        AVERAGE,
        /**
         * Selects the lowest non-null value.
         */
        MIN,
        /**
         * Selects the highest non-null value.
         */
        MAX;

        @Override
        public @Nullable Object reduce(List<@Nullable Object> values, boolean hasValue) {
            return TimeSeriesAggregation.reduceStandard(this, values, hasValue);
        }
    }
}
