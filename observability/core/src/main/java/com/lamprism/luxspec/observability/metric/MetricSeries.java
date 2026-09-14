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

package com.lamprism.luxspec.observability.metric;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One immutable metric series and its raw samples.
 *
 * <p>{@link #group(Collection)} and {@link #group(Collection, int)} are projection helpers for
 * dashboard code. They group by metric name and complete formatted dimensions, order samples by
 * timestamp and snapshot sequence, and preserve duplicate raw samples. The point limit applies to
 * every series independently and keeps the earliest samples in that chronological order.</p>
 *
 * @author RollW
 */
public final class MetricSeries {
    private static final Comparator<MetricSample> SAMPLE_ORDER =
            Comparator.comparing(MetricSample::getTimestamp)
                    .thenComparingLong(MetricSample::getSequence);

    private final MetricSeriesKey key;
    private final List<MetricSample> samples;

    private MetricSeries(MetricSeriesKey key, List<MetricSample> samples) {
        this.key = Objects.requireNonNull(key, "key");
        this.samples = List.copyOf(samples);
    }

    /**
     * Groups all supplied samples without a point limit.
     *
     * @param samples the raw samples to group
     * @return immutable series in first-seen key order
     */
    public static List<MetricSeries> group(Collection<? extends MetricSample> samples) {
        return group(samples, Integer.MAX_VALUE);
    }

    /**
     * Groups supplied samples and limits every series independently.
     *
     * <p>The input should normally be the complete result of a time-series query. A globally
     * bounded query result may already have omitted points from some series; this helper does not
     * issue another query. It still orders each group so callers can pass samples from a custom
     * store in any order.</p>
     *
     * @param samples       the raw samples to group
     * @param maximumPoints the maximum number of samples retained in each series
     * @return immutable series in first-seen key order
     */
    public static List<MetricSeries> group(
            Collection<? extends MetricSample> samples,
            int maximumPoints
    ) {
        Objects.requireNonNull(samples, "samples");
        if (maximumPoints < 1) {
            throw new IllegalArgumentException("maximumPoints must be positive");
        }

        Map<MetricSeriesKey, List<MetricSample>> grouped = new LinkedHashMap<>();
        for (MetricSample sample : samples) {
            MetricSample nonNullSample = Objects.requireNonNull(sample, "sample");
            grouped.computeIfAbsent(nonNullSample.getSeriesKey(), ignored -> new ArrayList<>())
                    .add(nonNullSample);
        }

        List<MetricSeries> result = new ArrayList<>(grouped.size());
        for (Map.Entry<MetricSeriesKey, List<MetricSample>> entry : grouped.entrySet()) {
            List<MetricSample> ordered = new ArrayList<>(entry.getValue());
            ordered.sort(SAMPLE_ORDER);
            if (ordered.size() > maximumPoints) {
                ordered = new ArrayList<>(ordered.subList(0, maximumPoints));
            }
            result.add(new MetricSeries(entry.getKey(), ordered));
        }
        return List.copyOf(result);
    }

    /**
     * Returns the stable series identity.
     *
     * @return the series key
     */
    public MetricSeriesKey getKey() {
        return key;
    }

    /**
     * Returns the retained raw samples in chronological order.
     *
     * @return immutable series samples
     */
    public List<MetricSample> getSamples() {
        return samples;
    }
}
