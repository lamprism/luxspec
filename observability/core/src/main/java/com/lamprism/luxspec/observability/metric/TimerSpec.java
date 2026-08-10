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
 * Declares a cumulative timer.
 *
 * @author RollW
 */
public final class TimerSpec extends AbstractMetricSpec<Timer> {
    private final DistributionSpec distribution;

    private TimerSpec(Builder builder) {
        super(builder.name(), builder.kind(), builder.dimensions(), builder.description(), builder.baseUnit(), builder.cardinalityPolicy());
        distribution = builder.distribution;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public DistributionSpec distribution() {
        return distribution;
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other) && distribution.equals(((TimerSpec) other).distribution);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), distribution);
    }

    /**
     * Builds a timer specification.
     */
    public static final class Builder extends BuilderSupport<Timer, Builder> {
        private DistributionSpec distribution = DistributionSpec.builder().build();

        private Builder(String name) {
            super(name, MetricKind.TIMER);
        }

        public Builder distribution(DistributionSpec distribution) {
            this.distribution = Objects.requireNonNull(distribution, "distribution");
            return this;
        }

        public TimerSpec build() {
            return new TimerSpec(this);
        }
    }
}
