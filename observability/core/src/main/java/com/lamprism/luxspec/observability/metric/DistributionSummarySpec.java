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

import java.util.Objects;

/**
 * Declares a cumulative numeric distribution summary.
 *
 * @author RollW
 */
public final class DistributionSummarySpec extends AbstractMetricSpec<DistributionSummary> {
    private final DistributionSpec distribution;
    private final boolean nonNegative;

    private DistributionSummarySpec(Builder builder) {
        super(builder.name(), builder.kind(), builder.dimensions(), builder.description(), builder.baseUnit(), builder.cardinalityPolicy());
        distribution = builder.distribution;
        nonNegative = builder.nonNegative;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public DistributionSpec distribution() {
        return distribution;
    }

    public boolean nonNegative() {
        return nonNegative;
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && distribution.equals(((DistributionSummarySpec) other).distribution)
                && nonNegative == ((DistributionSummarySpec) other).nonNegative;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), distribution, nonNegative);
    }

    /**
     * Builds a distribution summary specification.
     */
    public static final class Builder extends BuilderSupport<DistributionSummary, Builder> {
        private DistributionSpec distribution = DistributionSpec.builder().build();
        private boolean nonNegative;

        private Builder(String name) {
            super(name, MetricKind.DISTRIBUTION_SUMMARY);
        }

        public Builder distribution(DistributionSpec distribution) {
            this.distribution = Objects.requireNonNull(distribution, "distribution");
            return this;
        }

        public Builder nonNegative() {
            nonNegative = true;
            return this;
        }

        public DistributionSummarySpec build() {
            return new DistributionSummarySpec(this);
        }
    }
}
