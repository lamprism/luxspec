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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Portable cumulative distribution options for timer and summary specs.
 *
 * <p>SLO boundaries must be finite and non-negative, and must be unique after
 * sorting. Scale must be finite and positive. Expected value bounds must be
 * finite and ordered from minimum to maximum. Supplying SLOs enables histogram
 * collection.
 *
 * @author RollW
 */
public final class DistributionSpec {
    private final boolean histogram;
    private final List<Double> serviceLevelObjectives;
    private final double scale;
    private final Double minimumExpectedValue;
    private final Double maximumExpectedValue;

    private DistributionSpec(Builder builder) {
        this.histogram = builder.histogram;
        this.serviceLevelObjectives = List.copyOf(builder.serviceLevelObjectives);
        this.scale = builder.scale;
        this.minimumExpectedValue = builder.minimumExpectedValue;
        this.maximumExpectedValue = builder.maximumExpectedValue;
    }

    /**
     * Starts a distribution configuration.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public boolean histogram() {
        return histogram;
    }

    public List<Double> serviceLevelObjectives() {
        return serviceLevelObjectives;
    }

    public double scale() {
        return scale;
    }

    public Double minimumExpectedValue() {
        return minimumExpectedValue;
    }

    public Double maximumExpectedValue() {
        return maximumExpectedValue;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DistributionSpec spec)) {
            return false;
        }
        return histogram == spec.histogram
                && serviceLevelObjectives.equals(spec.serviceLevelObjectives)
                && Double.compare(scale, spec.scale) == 0
                && Objects.equals(minimumExpectedValue, spec.minimumExpectedValue)
                && Objects.equals(maximumExpectedValue, spec.maximumExpectedValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(histogram, serviceLevelObjectives, scale, minimumExpectedValue, maximumExpectedValue);
    }

    /**
     * Builds an immutable distribution configuration.
     */
    public static final class Builder {
        private boolean histogram;
        private final List<Double> serviceLevelObjectives = new ArrayList<>();
        private double scale = 1.0d;
        private Double minimumExpectedValue;
        private Double maximumExpectedValue;

        private Builder() {
        }

        public Builder histogram() {
            histogram = true;
            return this;
        }

        public Builder serviceLevelObjectives(Duration... boundaries) {
            Objects.requireNonNull(boundaries, "boundaries");
            for (Duration boundary : boundaries) {
                Objects.requireNonNull(boundary, "boundary");
                if (boundary.isNegative()) {
                    throw new IllegalArgumentException("SLO boundaries must not be negative");
                }
                serviceLevelObjectives.add(seconds(boundary));
            }
            histogram = true;
            return this;
        }

        public Builder serviceLevelObjectives(double... boundaries) {
            Objects.requireNonNull(boundaries, "boundaries");
            for (double boundary : boundaries) {
                requireFiniteNonNegative(boundary, "SLO boundary");
                serviceLevelObjectives.add(boundary);
            }
            histogram = true;
            return this;
        }

        public Builder scale(double scale) {
            if (!Double.isFinite(scale) || scale <= 0.0d) {
                throw new IllegalArgumentException("scale must be finite and positive");
            }
            this.scale = scale;
            return this;
        }

        public Builder expectedValueBounds(double minimum, double maximum) {
            requireFinite(minimum, "minimum");
            requireFinite(maximum, "maximum");
            if (minimum > maximum) {
                throw new IllegalArgumentException("minimum must not exceed maximum");
            }
            minimumExpectedValue = minimum;
            maximumExpectedValue = maximum;
            return this;
        }

        public DistributionSpec build() {
            List<Double> sorted = new ArrayList<>(serviceLevelObjectives);
            sorted.sort(Double::compareTo);
            for (int index = 1; index < sorted.size(); index++) {
                if (Double.compare(sorted.get(index - 1), sorted.get(index)) == 0) {
                    throw new IllegalArgumentException("SLO boundaries must be unique");
                }
            }
            serviceLevelObjectives.clear();
            serviceLevelObjectives.addAll(sorted);
            return new DistributionSpec(this);
        }

        private static void requireFinite(double value, String name) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException(name + " must be finite");
            }
        }

        private static void requireFiniteNonNegative(double value, String name) {
            requireFinite(value, name);
            if (value < 0.0d) {
                throw new IllegalArgumentException(name + " must not be negative");
            }
        }

        private static double seconds(Duration duration) {
            return duration.getSeconds() + duration.getNano() / 1_000_000_000.0d;
        }
    }
}
