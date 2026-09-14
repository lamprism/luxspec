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
 * Selects metric declarations for one Registry lifetime.
 *
 * @author RollW
 */
@FunctionalInterface
public interface MetricActivation {
    /**
     * Selects every declared metric.
     *
     * @return the all-metrics activation
     */
    static MetricActivation all() {
        return spec -> true;
    }

    /**
     * Selects declarations with one exact metric name.
     *
     * @param name the selected metric name
     * @return the name-based activation
     */
    static MetricActivation byName(MetricName name) {
        MetricName nonNullName = Objects.requireNonNull(name, "name");
        return spec -> spec.getName().equals(nonNullName);
    }

    /**
     * Reports whether one metric declaration is enabled for the registry.
     *
     * @param spec the metric declaration
     * @return {@code true} when the declaration should be registered
     */
    boolean isEnabled(MetricSpec<?> spec);
}
