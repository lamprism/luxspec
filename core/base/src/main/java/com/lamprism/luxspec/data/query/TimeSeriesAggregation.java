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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Applies the selected reduction semantics of a {@link TimeSeriesQueryPlan}.
 *
 * <p>Both in-memory and persistence adapters use this class after obtaining the typed source
 * values. Standard reductions keep null filtering, decimal summation, averaging, and comparable
 * extrema consistent across providers.</p>
 *
 * @author RollW
 */
public final class TimeSeriesAggregation {
    private TimeSeriesAggregation() {
    }

    /**
     * Reduces values according to the plan.
     *
     * @param plan   the time-series operation embedded in a query plan
     * @param values the source values in ascending timestamp order within one time bucket
     * @return the reduced value, or null when the bucket contains no reducible value
     */
    public static @Nullable Object reduce(
            TimeSeriesQueryPlan<?> plan,
            List<?> values
    ) {
        TimeSeriesQueryPlan<?> nonNullPlan = Objects.requireNonNull(plan, "plan");
        List<@Nullable Object> copied = new ArrayList<>(Objects.requireNonNull(values, "values"));
        Object result = nonNullPlan.getAggregation().reduce(
                Collections.unmodifiableList(copied), nonNullPlan.getValue() != null
        );
        return result == null ? null : nonNullPlan.getValueType().cast(result);
    }

    static @Nullable Object reduceStandard(
            TimeSeriesAggregationFunction.Standard function, List<@Nullable Object> values, boolean hasValue
    ) {
        List<Object> presentValues = new ArrayList<>(values.size());
        for (Object value : values) {
            if (value != null) {
                presentValues.add(value);
            }
        }
        return switch (function) {
            case COUNT -> hasValue ? (long) presentValues.size() : (long) values.size();
            case COUNT_DISTINCT -> (long) new LinkedHashSet<>(presentValues).size();
            case SUM -> sum(presentValues);
            case AVERAGE -> average(presentValues);
            case MIN -> extreme(presentValues, false);
            case MAX -> extreme(presentValues, true);
        };
    }

    private static @Nullable BigDecimal sum(List<Object> values) {
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal result = BigDecimal.ZERO;
        for (Object value : values) {
            if (!(value instanceof Number number)) {
                throw new IllegalArgumentException("SUM requires numeric values");
            }
            result = result.add(toBigDecimal(number));
        }
        return result;
    }

    private static @Nullable Double average(List<Object> values) {
        if (values.isEmpty()) {
            return null;
        }
        double result = 0.0d;
        for (Object value : values) {
            if (!(value instanceof Number number)) {
                throw new IllegalArgumentException("AVERAGE requires numeric values");
            }
            result += number.doubleValue();
        }
        return result / values.size();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static @Nullable Object extreme(List<Object> values, boolean maximum) {
        if (values.isEmpty()) {
            return null;
        }
        Object result = values.get(0);
        if (!(result instanceof Comparable comparable)) {
            throw new IllegalArgumentException("MIN and MAX require comparable values");
        }
        for (int index = 1; index < values.size(); index++) {
            Object candidate = values.get(index);
            if (!(candidate instanceof Comparable comparableCandidate)) {
                throw new IllegalArgumentException("MIN and MAX require comparable values");
            }
            int comparison = comparable.compareTo(candidate);
            if ((maximum && comparison < 0) || (!maximum && comparison > 0)) {
                result = candidate;
                comparable = comparableCandidate;
            }
        }
        return result;
    }

    private static BigDecimal toBigDecimal(Number value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(value.toString());
    }
}
