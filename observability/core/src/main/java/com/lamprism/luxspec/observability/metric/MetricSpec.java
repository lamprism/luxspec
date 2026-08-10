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

import java.util.List;

/**
 * Immutable declaration of one logical metric definition.
 *
 * <p>Implementations provide the semantic name, typed instrument kind,
 * dimension schema, and collection policies. The built-in runtime supports
 * the specialized Core spec types; a custom implementation is not
 * automatically materialized as a new instrument kind.
 *
 * @author RollW
 */
public interface MetricSpec<M extends Metric> {
    MetricName getName();

    MetricKind<M> getKind();

    List<MetricDimensionSpec<?>> getDimensions();

    @Nullable
    MetricDescription getDescription();

    @Nullable
    MetricUnit getBaseUnit();

    @Nullable
    MetricCardinalityPolicy getCardinalityPolicy();

    MetricBinding<M> bind(MetricDimensionSet dimensions);

    default MetricBinding<M> bind() {
        return bind(MetricDimensionSet.empty());
    }
}
