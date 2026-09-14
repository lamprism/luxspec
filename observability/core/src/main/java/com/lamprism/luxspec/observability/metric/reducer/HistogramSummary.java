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

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * An immutable histogram projection from one metric series.
 *
 * <p>Bucket counts are expected to be cumulative within the returned map. Quantiles are
 * upper-bound estimates: the returned bucket boundary is the first cumulative bucket that
 * reaches the requested rank. If the requested rank is above the last configured bucket, the
 * quantile is unavailable.</p>
 *
 * @author RollW
 */
public final class HistogramSummary {
    private final NavigableMap<Double, Long> buckets;
    private final @Nullable Long totalCount;

    HistogramSummary(Map<Double, Long> buckets, @Nullable Long totalCount) {
        TreeMap<Double, Long> ordered = new TreeMap<>();
        for (Map.Entry<Double, Long> entry : buckets.entrySet()) {
            Double upperBound = entry.getKey();
            Long count = entry.getValue();
            if (!Double.isFinite(upperBound)) {
                throw new IllegalArgumentException("histogram bucket upper bound must be finite");
            }
            if (count == null || count < 0L) {
                throw new IllegalArgumentException("histogram bucket counts must be cumulative and non-negative");
            }
            ordered.put(upperBound, count);
        }
        long previousCount = 0L;
        for (long count : ordered.values()) {
            if (count < previousCount) {
                throw new IllegalArgumentException("histogram bucket counts must be cumulative and non-negative");
            }
            previousCount = count;
        }
        if (totalCount != null) {
            if (totalCount < 0L || (!ordered.isEmpty() && previousCount > totalCount)) {
                throw new IllegalArgumentException("histogram total count is inconsistent with buckets");
            }
        }
        this.buckets = Collections.unmodifiableNavigableMap(ordered);
        this.totalCount = totalCount;
    }

    /**
     * Returns cumulative bucket counts ordered by upper bound.
     *
     * @return immutable histogram buckets
     */
    public Map<Double, Long> getBuckets() {
        return buckets;
    }

    /**
     * Returns the source reading count when it was available.
     *
     * @return the total count, or null when the source did not provide one
     */
    public @Nullable Long getTotalCount() {
        return totalCount;
    }

    /**
     * Estimates a quantile using the cumulative bucket upper bounds.
     *
     * @param quantile a value between zero and one, inclusive
     * @return the first bucket upper bound reaching the requested rank, or null when unavailable
     */
    public @Nullable Double getQuantile(double quantile) {
        if (!Double.isFinite(quantile) || quantile < 0.0d || quantile > 1.0d) {
            throw new IllegalArgumentException("quantile must be between zero and one");
        }
        if (buckets.isEmpty()) {
            return null;
        }
        long count = totalCount == null ? buckets.lastEntry().getValue() : totalCount;
        if (count <= 0L) {
            return null;
        }
        long targetRank = Math.max(1L, (long) Math.ceil(quantile * count));
        for (Map.Entry<Double, Long> entry : buckets.entrySet()) {
            if (entry.getValue() >= targetRank) {
                return entry.getKey();
            }
        }
        return null;
    }
}
