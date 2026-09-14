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
import java.util.Map;
import java.util.TreeMap;

/**
 * Reduces cumulative histograms from one metric series.
 *
 * <p>Cumulative buckets are differenced between consecutive samples. A decrease in a bucket or
 * total count is treated as a reset, and the value after the reset is counted as the increment
 * since that reset. The resulting histogram represents the interval between the first and last
 * sample.</p>
 *
 * @author RollW
 */
public final class HistogramSampleReducer implements MetricReducer<HistogramSummary> {
    /**
     * Creates a histogram sample reducer.
     */
    public HistogramSampleReducer() {
    }

    /**
     * Calculates the reset-aware histogram delta for the supplied series.
     *
     * @param samples the raw samples from one histogram series
     * @return the immutable interval histogram summary
     */
    @Override
    public HistogramSummary reduce(Collection<? extends MetricSample> samples) {
        List<MetricSample> ordered = MetricSampleOrdering.copyAndSort(samples);
        if (ordered.size() < 2) {
            if (ordered.isEmpty()) {
                return new HistogramSummary(Map.of(), null);
            }
            HistogramSummary single = snapshot(ordered.get(0));
            return new HistogramSummary(
                    Map.of(),
                    single.getTotalCount() == null ? null : 0L
            );
        }

        HistogramSummary previous = snapshot(ordered.get(0));
        Map<Double, Long> deltaBuckets = new TreeMap<>();
        long totalDelta = 0L;
        boolean totalAvailable = previous.getTotalCount() != null;
        for (int index = 1; index < ordered.size(); index++) {
            HistogramSummary current = snapshot(ordered.get(index));
            addBucketDelta(deltaBuckets, previous.getBuckets(), current.getBuckets());
            if (totalAvailable) {
                Long previousTotal = previous.getTotalCount();
                Long currentTotal = current.getTotalCount();
                if (previousTotal == null || currentTotal == null) {
                    totalAvailable = false;
                } else {
                    long increment = currentTotal < previousTotal ? currentTotal : currentTotal - previousTotal;
                    totalDelta = addExact(totalDelta, increment, "histogram count delta");
                }
            }
            previous = current;
        }
        return new HistogramSummary(deltaBuckets, totalAvailable ? totalDelta : null);
    }

    private static HistogramSummary snapshot(MetricSample sample) {
        return new HistogramSummary(sample.getHistogram(), sample.getCount());
    }

    private static void addBucketDelta(
            Map<Double, Long> deltaBuckets,
            Map<Double, Long> previous,
            Map<Double, Long> current
    ) {
        TreeMap<Double, Long> boundaries = new TreeMap<>(previous);
        boundaries.putAll(current);
        for (double boundary : boundaries.keySet()) {
            long previousCount = previous.getOrDefault(boundary, 0L);
            long currentCount = current.getOrDefault(boundary, 0L);
            long increment = currentCount < previousCount ? currentCount : currentCount - previousCount;
            long updated = addExact(
                    deltaBuckets.getOrDefault(boundary, 0L),
                    increment,
                    "histogram bucket delta"
            );
            if (updated == 0L) {
                deltaBuckets.remove(boundary);
            } else {
                deltaBuckets.put(boundary, updated);
            }
        }
    }

    private static long addExact(long left, long right, String name) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException failure) {
            throw new ArithmeticException(name + " overflowed");
        }
    }
}
