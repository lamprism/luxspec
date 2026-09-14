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
 * Reduces the scalar value of one cumulative counter series into delta and rate values.
 *
 * <p>Samples are copied and ordered by timestamp and sequence. Samples without a value are
 * ignored. A negative counter value is invalid; a lower value than the previous available value
 * is treated as a reset to zero followed by the current value.</p>
 *
 * @author RollW
 */
public final class CounterSampleReducer implements MetricReducer<CounterSummary> {
    /**
     * Creates a counter sample reducer.
     */
    public CounterSampleReducer() {
    }

    /**
     * Calculates a reset-aware counter delta and rate.
     *
     * @param samples the raw samples from one cumulative counter series
     * @return the immutable counter summary
     */
    @Override
    public CounterSummary reduce(Collection<? extends MetricSample> samples) {
        List<MetricSample> ordered = MetricSampleOrdering.copyAndSort(samples);
        int sampleCount = 0;
        int resetCount = 0;
        Double firstValue = null;
        Double lastValue = null;
        double delta = 0.0d;
        double previousValue = 0.0d;
        Instant firstTimestamp = null;
        Instant lastTimestamp = null;

        for (MetricSample sample : ordered) {
            Double value = sample.getValue();
            if (value == null) {
                continue;
            }
            requireCounterValue(value);
            if (sampleCount == 0) {
                firstValue = value;
                firstTimestamp = sample.getTimestamp();
            } else {
                double increment;
                if (value < previousValue) {
                    resetCount++;
                    increment = value;
                } else {
                    increment = value - previousValue;
                }
                delta = addFinite(delta, increment, "counter delta");
            }
            lastValue = value;
            previousValue = value;
            lastTimestamp = sample.getTimestamp();
            sampleCount++;
        }

        if (sampleCount == 0) {
            return new CounterSummary(0, null, null, null, null, 0);
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
                    throw new ArithmeticException("counter rate is not finite");
                }
            }
        }
        return new CounterSummary(sampleCount, firstValue, lastValue, delta, ratePerSecond, resetCount);
    }

    private static void requireCounterValue(double value) {
        if (!Double.isFinite(value) || value < 0.0d) {
            throw new IllegalArgumentException("counter value must be finite and non-negative");
        }
    }

    private static double addFinite(double left, double right, String name) {
        double result = left + right;
        if (!Double.isFinite(result)) {
            throw new ArithmeticException(name + " is not finite");
        }
        return result;
    }
}
