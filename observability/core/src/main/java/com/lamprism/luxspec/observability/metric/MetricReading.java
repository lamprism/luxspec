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

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * One immutable reading captured from one metric binding.
 *
 * @author RollW
 */
public final class MetricReading {
    private final MetricBinding<?> binding;
    private final MetricKind<?> kind;
    private final @Nullable Double value;
    private final @Nullable Long count;
    private final @Nullable Double total;
    private final @Nullable Double max;
    private final @Nullable Duration totalTime;
    private final @Nullable Duration activeDuration;
    private final @Nullable Long activeTasks;
    private final Map<Double, Long> histogram;

    private MetricReading(Builder builder) {
        binding = Objects.requireNonNull(builder.binding, "binding");
        kind = Objects.requireNonNull(builder.kind, "kind");
        value = builder.value;
        count = builder.count;
        total = builder.total;
        max = builder.max;
        totalTime = builder.totalTime;
        activeDuration = builder.activeDuration;
        activeTasks = builder.activeTasks;
        histogram = Map.copyOf(builder.histogram);
    }

    public static Builder builder(MetricBinding<?> binding, MetricKind<?> kind) {
        return new Builder(binding, kind);
    }

    public MetricBinding<?> binding() {
        return binding;
    }

    public MetricBinding<?> getBinding() {
        return binding;
    }

    public MetricKind<?> kind() {
        return kind;
    }

    public MetricKind<?> getKind() {
        return kind;
    }

    public @Nullable Double value() {
        return value;
    }

    public @Nullable Double getValue() {
        return value;
    }

    public @Nullable Long count() {
        return count;
    }

    public @Nullable Long getCount() {
        return count;
    }

    public @Nullable Double total() {
        return total;
    }

    public @Nullable Double getTotal() {
        return total;
    }

    public @Nullable Double max() {
        return max;
    }

    public @Nullable Double getMax() {
        return max;
    }

    public @Nullable Duration totalTime() {
        return totalTime;
    }

    public @Nullable Duration getTotalTime() {
        return totalTime;
    }

    public @Nullable Duration activeDuration() {
        return activeDuration;
    }

    public @Nullable Duration getActiveDuration() {
        return activeDuration;
    }

    public @Nullable Long activeTasks() {
        return activeTasks;
    }

    public @Nullable Long getActiveTasks() {
        return activeTasks;
    }

    public Map<Double, Long> histogram() {
        return histogram;
    }

    /**
     * Builds one immutable reading.
     */
    public static final class Builder {
        private final MetricBinding<?> binding;
        private final MetricKind<?> kind;
        private @Nullable Double value;
        private @Nullable Long count;
        private @Nullable Double total;
        private @Nullable Double max;
        private @Nullable Duration totalTime;
        private @Nullable Duration activeDuration;
        private @Nullable Long activeTasks;
        private Map<Double, Long> histogram = Map.of();

        private Builder(MetricBinding<?> binding, MetricKind<?> kind) {
            this.binding = Objects.requireNonNull(binding, "binding");
            this.kind = Objects.requireNonNull(kind, "kind");
        }

        public Builder value(@Nullable Double value) {
            this.value = value;
            return this;
        }

        public Builder count(@Nullable Long count) {
            this.count = count;
            return this;
        }

        public Builder total(@Nullable Double total) {
            this.total = total;
            return this;
        }

        public Builder max(@Nullable Double max) {
            this.max = max;
            return this;
        }

        public Builder totalTime(@Nullable Duration totalTime) {
            this.totalTime = totalTime;
            return this;
        }

        public Builder activeDuration(@Nullable Duration activeDuration) {
            this.activeDuration = activeDuration;
            return this;
        }

        public Builder activeTasks(@Nullable Long activeTasks) {
            this.activeTasks = activeTasks;
            return this;
        }

        public Builder histogram(Map<Double, Long> histogram) {
            this.histogram = Map.copyOf(Objects.requireNonNull(histogram, "histogram"));
            return this;
        }

        public MetricReading build() {
            return new MetricReading(this);
        }
    }
}
