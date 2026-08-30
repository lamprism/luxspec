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
 * An immutable semantic metric name.
 *
 * <p>{@link #of(String)} trims surrounding characters and rejects null, empty,
 * and ISO control characters. It preserves case and all other characters; it
 * does not require Prometheus syntax, lowercase text, or an application prefix.
 *
 * @author RollW
 */
public final class MetricName {
    private static final MetricTextNormalizer VALUE_NORMALIZER = MetricTextNormalizer.INSTANCE;

    private final String value;

    private MetricName(String value) {
        this.value = value;
    }

    /**
     * Creates a validated metric name.
     *
     * @param value the semantic name
     * @return the metric name
     */
    public static MetricName of(String value) {
        return new MetricName(VALUE_NORMALIZER.normalize(value));
    }

    /**
     * Returns the semantic name.
     *
     * @return the name
     */
    public String value() {
        return value;
    }

    /**
     * Returns the semantic name.
     *
     * @return the name
     */
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MetricName name)) {
            return false;
        }
        return value.equals(name.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
