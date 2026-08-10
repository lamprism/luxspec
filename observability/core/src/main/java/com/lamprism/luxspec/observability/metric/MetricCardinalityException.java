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

/**
 * Indicates that a metric definition has reached its configured cardinality limit.
 *
 * @author RollW
 */
public final class MetricCardinalityException extends RuntimeException {
    public MetricCardinalityException(MetricName name, int maximumBindings) {
        super("Metric binding cardinality limit reached for " + name + ": " + maximumBindings);
    }
}
