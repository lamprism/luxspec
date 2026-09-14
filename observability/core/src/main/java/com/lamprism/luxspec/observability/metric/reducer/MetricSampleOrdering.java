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
import com.lamprism.luxspec.observability.metric.MetricSeriesKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

final class MetricSampleOrdering {
    private static final Comparator<MetricSample> SAMPLE_ORDER =
            Comparator.comparing(MetricSample::getTimestamp)
                    .thenComparingLong(MetricSample::getSequence);

    private MetricSampleOrdering() {
    }

    static List<MetricSample> copyAndSort(Collection<? extends MetricSample> samples) {
        Objects.requireNonNull(samples, "samples");
        List<MetricSample> ordered = new ArrayList<>(samples.size());
        MetricSeriesKey seriesKey = null;
        for (MetricSample sample : samples) {
            MetricSample nonNullSample = Objects.requireNonNull(sample, "sample");
            if (seriesKey == null) {
                seriesKey = nonNullSample.getSeriesKey();
            } else if (!seriesKey.equals(nonNullSample.getSeriesKey())) {
                throw new IllegalArgumentException("samples must belong to one metric series");
            }
            ordered.add(nonNullSample);
        }
        ordered.sort(SAMPLE_ORDER);
        return ordered;
    }
}
