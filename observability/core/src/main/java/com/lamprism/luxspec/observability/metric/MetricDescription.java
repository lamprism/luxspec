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
 * Optional human-readable metric description.
 *
 * <p>{@link #of(String)} trims surrounding characters and rejects null, empty,
 * and ISO control characters. The description is not assigned provider-specific
 * escaping or a maximum length by Core.
 *
 * @author RollW
 */
public final class MetricDescription {
    private static final MetricTextNormalizer VALUE_NORMALIZER = MetricTextNormalizer.INSTANCE;

    private final String value;

    private MetricDescription(String value) {
        this.value = value;
    }

    public static MetricDescription of(String value) {
        return new MetricDescription(VALUE_NORMALIZER.normalize(value));
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MetricDescription description)) {
            return false;
        }
        return value.equals(description.value);
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
