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

import com.lamprism.luxspec.observability.metric.MetricSample;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Reduces the cumulative {@link MetricSample#getCount()} field for one metric series.
 *
 * <p>This reducer is useful for timer, distribution summary, and function timer throughput. It
 * orders samples by timestamp and sequence, ignores samples without a count, and treats a lower
 * count as a reset.</p>
 *
 * @author RollW
 */
public final class MetricSampleCountReducer implements MetricReducer<CountSummary> {
    /**
     * Creates a metric count sample reducer.
     */
    public MetricSampleCountReducer() {
    }

    /**
     * Calculates a reset-aware count delta and rate.
     *
     * @param samples the raw samples from one cumulative count series
     * @return the immutable count summary
     */
    @Override
    public CountSummary reduce(Collection<? extends MetricSample> samples) {
        List<MetricSample> ordered = MetricSampleOrdering.copyAndSort(samples);
        int sampleCount = 0;
        int resetCount = 0;
        Long firstValue = null;
        Long lastValue = null;
        long previousValue = 0L;
        long delta = 0L;
        Instant firstTimestamp = null;
        Instant lastTimestamp = null;

        for (MetricSample sample : ordered) {
            Long value = sample.getCount();
            if (value == null) {
                continue;
            }
            requireCount(value);
            if (sampleCount == 0) {
                firstValue = value;
                firstTimestamp = sample.getTimestamp();
            } else {
                long increment;
                if (value < previousValue) {
                    resetCount++;
                    increment = value;
                } else {
                    increment = value - previousValue;
                }
                delta = addExact(delta, increment);
            }
            lastValue = value;
            previousValue = value;
            lastTimestamp = sample.getTimestamp();
            sampleCount++;
        }

        if (sampleCount == 0) {
            return new CountSummary(0, null, null, null, null, 0);
        }

        Double ratePerSecond = null;
        if (sampleCount > 1) {
            Duration elapsed = Duration.between(
                    Objects.requireNonNull(firstTimestamp, "firstTimestamp"),
                    Objects.requireNonNull(lastTimestamp, "lastTimestamp")
            );
            double elapsedSeconds = elapsed.getSeconds() + elapsed.getNano() / 1_000_000_000.0d;
            if (elapsedSeconds > 0.0d && Double.isFinite(elapsedSeconds)) {
                ratePerSecond = delta / elapsedSeconds;
                if (!Double.isFinite(ratePerSecond)) {
                    throw new ArithmeticException("count rate is not finite");
                }
            }
        }
        return new CountSummary(sampleCount, firstValue, lastValue, delta, ratePerSecond, resetCount);
    }

    private static void requireCount(long value) {
        if (value < 0L) {
            throw new IllegalArgumentException("metric count must not be negative");
        }
    }

    private static long addExact(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException failure) {
            throw new ArithmeticException("metric count delta overflowed");
        }
    }
}
