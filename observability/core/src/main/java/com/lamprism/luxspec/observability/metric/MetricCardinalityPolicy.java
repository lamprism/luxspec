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
 * Optional per-spec bound on materialized metric bindings.
 *
 * <p>The maximum binding count must be positive and applies to distinct
 * complete bindings for one metric specification. The overflow behavior is
 * explicit and is never inferred from an exporter.
 *
 * <p>Use this policy when a metric has dynamic dimensions, such as a value
 * derived from a request or tenant, and materializing every distinct binding
 * could consume unbounded memory. It is usually unnecessary for dimensions
 * with a small, finite set of allowed values.
 *
 * <p>Attach the policy to the metric specification and choose the behavior
 * for a new binding after the limit is reached:
 *
 * <pre>{@code
 * CounterSpec requests = CounterSpec.builder("http.requests")
 *         .cardinality(MetricCardinalityPolicy.bounded(
 *                 1_000, CardinalityOverflow.DROP))
 *         .build();
 * }</pre>
 *
 * <p>The limit is evaluated by one {@link MetricRegistry} for each metric
 * specification and counts complete bindings that have been materialized.
 * {@link CardinalityOverflow#DROP} returns a no-op handle for later bindings;
 * {@link CardinalityOverflow#REJECT} throws an explicit exception. The
 * registry does not evict existing bindings or create an overflow binding.
 *
 * @author RollW
 */
public final class MetricCardinalityPolicy {
    private final int maximumBindings;
    private final CardinalityOverflow overflow;

    private MetricCardinalityPolicy(int maximumBindings, CardinalityOverflow overflow) {
        if (maximumBindings < 1) {
            throw new IllegalArgumentException("maximumBindings must be positive");
        }
        this.maximumBindings = maximumBindings;
        this.overflow = Objects.requireNonNull(overflow, "overflow");
    }

    /**
     * Creates a bounded cardinality policy.
     *
     * @param maximumBindings the maximum number of distinct materialized bindings
     * @param overflow        the overflow behavior
     * @return the policy
     */
    public static MetricCardinalityPolicy bounded(int maximumBindings, CardinalityOverflow overflow) {
        return new MetricCardinalityPolicy(maximumBindings, overflow);
    }

    /**
     * Creates a drop-on-overflow policy.
     *
     * @param maximumBindings the maximum number of distinct materialized bindings
     * @return the policy
     */
    public static MetricCardinalityPolicy bounded(int maximumBindings) {
        return bounded(maximumBindings, CardinalityOverflow.DROP);
    }

    public int maximumBindings() {
        return maximumBindings;
    }

    public CardinalityOverflow overflow() {
        return overflow;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MetricCardinalityPolicy policy)) {
            return false;
        }
        return maximumBindings == policy.maximumBindings && overflow == policy.overflow;
    }

    @Override
    public int hashCode() {
        return Objects.hash(maximumBindings, overflow);
    }
}
