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

import org.jspecify.annotations.Nullable;

/**
 * Basic aggregate values calculated from one metric series.
 *
 * <p>The sample count counts samples with an available scalar value. All value fields are
 * unavailable when that count is zero.</p>
 *
 * @author RollW
 */
public final class MetricValueSummary {
    private final int sampleCount;
    private final @Nullable Double firstValue;
    private final @Nullable Double lastValue;
    private final @Nullable Double minimum;
    private final @Nullable Double maximum;
    private final @Nullable Double sum;
    private final @Nullable Double average;

    MetricValueSummary(
            int sampleCount,
            @Nullable Double firstValue,
            @Nullable Double lastValue,
            @Nullable Double minimum,
            @Nullable Double maximum,
            @Nullable Double sum,
            @Nullable Double average
    ) {
        if (sampleCount < 0) {
            throw new IllegalArgumentException("sampleCount must not be negative");
        }
        this.sampleCount = sampleCount;
        this.firstValue = firstValue;
        this.lastValue = lastValue;
        this.minimum = minimum;
        this.maximum = maximum;
        this.sum = sum;
        this.average = average;
    }

    /**
     * Returns the number of samples with an available value.
     *
     * @return the available value count
     */
    public int getSampleCount() {
        return sampleCount;
    }

    /**
     * Returns the first value in chronological order when available.
     *
     * @return the first value, or null when no value was available
     */
    public @Nullable Double getFirstValue() {
        return firstValue;
    }

    /**
     * Returns the last value in chronological order when available.
     *
     * @return the last value, or null when no value was available
     */
    public @Nullable Double getLastValue() {
        return lastValue;
    }

    /**
     * Returns the minimum value when available.
     *
     * @return the minimum value, or null when no value was available
     */
    public @Nullable Double getMinimum() {
        return minimum;
    }

    /**
     * Returns the maximum value when available.
     *
     * @return the maximum value, or null when no value was available
     */
    public @Nullable Double getMaximum() {
        return maximum;
    }

    /**
     * Returns the sum when available.
     *
     * @return the sum, or null when no value was available
     */
    public @Nullable Double getSum() {
        return sum;
    }

    /**
     * Returns the arithmetic average when available.
     *
     * @return the average, or null when no value was available
     */
    public @Nullable Double getAverage() {
        return average;
    }
}
