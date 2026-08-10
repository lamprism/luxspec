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

package com.lamprism.luxspec.observability.observation;

import com.lamprism.luxspec.validation.ValidationRules;

import java.util.Objects;
import java.util.UUID;

/**
 * Opaque identity of one started observation.
 *
 * @author RollW
 */
public final class ObservationId {
    private final String value;

    private ObservationId(String value) {
        this.value = value;
    }

    public static ObservationId generated() {
        return new ObservationId(UUID.randomUUID().toString());
    }

    public static ObservationId of(String value) {
        String nonNullValue = Objects.requireNonNull(value, "value");
        ValidationRules.nonBlank("Observation ID").validate(nonNullValue);
        return new ObservationId(nonNullValue);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ObservationId id)) {
            return false;
        }
        return value.equals(id.value);
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
