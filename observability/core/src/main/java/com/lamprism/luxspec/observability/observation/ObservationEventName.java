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

import com.lamprism.luxspec.validation.Normalizer;
import com.lamprism.luxspec.validation.Validator;

/**
 * An immutable operation event name, distinct from an observation name.
 *
 * <p>{@link #of(String)} trims surrounding characters and rejects null, empty,
 * and ISO control characters. It preserves case and all other characters and
 * does not impose a vendor-specific naming syntax.
 *
 * @author RollW
 */
public final class ObservationEventName {
    private static final Normalizer<String> VALUE_NORMALIZER = Normalizer.of(String::trim)
            .validatedBy(
                    Validator.nonBlank("Observation event name")
                            .and(Validator.noControlCharacters("Observation event name"))
            );

    private final String value;

    private ObservationEventName(String value) {
        this.value = value;
    }

    public static ObservationEventName of(String value) {
        return new ObservationEventName(VALUE_NORMALIZER.normalize(value));
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ObservationEventName name)) {
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
