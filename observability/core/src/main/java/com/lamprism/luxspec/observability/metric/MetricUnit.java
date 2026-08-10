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
 * Optional provider-neutral metric unit.
 *
 * <p>{@link #of(String)} trims surrounding characters and rejects null, empty,
 * and ISO control characters. Core preserves the remaining value and does not
 * convert readings or impose a provider-specific unit vocabulary.
 *
 * @author RollW
 */
public final class MetricUnit {
    private final String value;

    private MetricUnit(String value) {
        this.value = value;
    }

    public static MetricUnit of(String value) {
        return new MetricUnit(NameValidation.require(value, "Metric unit"));
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MetricUnit unit)) {
            return false;
        }
        return value.equals(unit.value);
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
