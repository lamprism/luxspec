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

import java.util.Collection;
import java.util.List;

/**
 * Reduces the scalar {@link MetricSample#getValue()} field for one metric series.
 *
 * <p>Samples are copied and ordered by timestamp and sequence before reduction. Samples without a
 * value are ignored. Use a separate series for every metric dimension set before invoking this
 * reducer.</p>
 *
 * @author RollW
 */
public final class MetricSampleValueReducer implements MetricReducer<MetricValueSummary> {
    /**
     * Creates a scalar sample reducer.
     */
    public MetricSampleValueReducer() {
    }

    /**
     * Calculates basic scalar aggregates.
     *
     * @param samples the raw samples from one metric series
     * @return the immutable aggregate summary
     */
    @Override
    public MetricValueSummary reduce(Collection<? extends MetricSample> samples) {
        List<MetricSample> ordered = MetricSampleOrdering.copyAndSort(samples);
        int sampleCount = 0;
        Double firstValue = null;
        Double lastValue = null;
        double minimum = 0.0d;
        double maximum = 0.0d;
        double sum = 0.0d;
        for (MetricSample sample : ordered) {
            Double value = sample.getValue();
            if (value == null) {
                continue;
            }
            requireFinite(value, "metric value");
            if (sampleCount == 0) {
                firstValue = value;
                minimum = value;
                maximum = value;
            } else {
                minimum = Math.min(minimum, value);
                maximum = Math.max(maximum, value);
            }
            double updatedSum = sum + value;
            if (!Double.isFinite(updatedSum)) {
                throw new ArithmeticException("metric value sum is not finite");
            }
            sum = updatedSum;
            lastValue = value;
            sampleCount++;
        }

        if (sampleCount == 0) {
            return new MetricValueSummary(0, null, null, null, null, null, null);
        }
        return new MetricValueSummary(
                sampleCount,
                firstValue,
                lastValue,
                minimum,
                maximum,
                sum,
                sum / sampleCount
        );
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
