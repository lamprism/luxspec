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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Identifies one metric series by its name and complete formatted dimensions.
 *
 * <p>The key intentionally uses formatted dimension values rather than the typed binding values.
 * This is the same identity exposed by metric samples and keeps a series key independent of the
 * metric implementation that produced it.</p>
 *
 * @author RollW
 */
public final class MetricSeriesKey {
    private final MetricName metricName;
    private final Map<String, String> dimensions;

    /**
     * Creates an immutable metric series key.
     *
     * @param metricName the metric name
     * @param dimensions the complete formatted dimensions
     */
    public MetricSeriesKey(MetricName metricName, Map<String, String> dimensions) {
        this.metricName = Objects.requireNonNull(metricName, "metricName");
        this.dimensions = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNull(dimensions, "dimensions")));
    }

    /**
     * Returns the metric name.
     *
     * @return the metric name
     */
    public MetricName getMetricName() {
        return metricName;
    }

    /**
     * Returns all formatted dimensions.
     *
     * @return immutable formatted dimensions
     */
    public Map<String, String> getDimensions() {
        return dimensions;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MetricSeriesKey key)) {
            return false;
        }
        return metricName.equals(key.metricName) && dimensions.equals(key.dimensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metricName, dimensions);
    }

    @Override
    public String toString() {
        return metricName.getValue() + dimensions;
    }
}
