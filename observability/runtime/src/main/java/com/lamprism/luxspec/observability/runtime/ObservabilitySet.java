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

package com.lamprism.luxspec.observability.runtime;

import com.lamprism.luxspec.observability.metric.MetricRegistry;
import com.lamprism.luxspec.observability.observation.ObservationRegistry;
import com.lamprism.luxspec.observability.runtime.health.HealthRegistryBuilder;

/**
 * Contributes one optional observability domain to the native registries.
 *
 * <p>A set may contribute metrics, observations, health contributors, or any combination of the
 * three. The same set can therefore be selected once at an assembly boundary without creating
 * parallel registration APIs for each provider.</p>
 *
 * @author RollW
 */
public interface ObservabilitySet {
    /**
     * Contributes metric declarations and materialized handles.
     *
     * @param registry the metric registry
     */
    default void registerMetrics(MetricRegistry registry) {
    }

    /**
     * Contributes operation observation declarations.
     *
     * @param registry the observation registry
     */
    default void registerObservations(ObservationRegistry registry) {
    }

    /**
     * Contributes health checks before the health registry is built.
     *
     * @param builder the health registry builder
     */
    default void registerHealth(HealthRegistryBuilder builder) {
    }
}
