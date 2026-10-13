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

import java.util.function.Consumer;

/**
 * Optional registry extension used by provider adapters to observe metric declarations and
 * materialization.
 *
 * @author RollW
 */
public interface MetricRegistryEventSource {
    /**
     * Adds a listener for a newly registered metric specification.
     *
     * @param listener the registration listener
     * @return a handle that removes the listener
     */
    AutoCloseable addRegistrationListener(Consumer<MetricSpec<?>> listener);

    /**
     * Adds a listener for a newly materialized metric.
     *
     * @param listener the materialization listener
     * @return a handle that removes the listener
     */
    AutoCloseable addMaterializationListener(Consumer<Metric> listener);
}
