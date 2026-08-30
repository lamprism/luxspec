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

package com.lamprism.luxspec.context;

import com.lamprism.luxspec.validation.Normalizer;
import com.lamprism.luxspec.validation.ValidationRules;

/**
 * A safe cross-module association value for one request or execution.
 *
 * <p>{@link #of(String)} trims surrounding characters and rejects null, empty,
 * whitespace, space, and ISO control characters. It does not require UUID
 * syntax. Generation is owned by the adapter that establishes the correlation
 * boundary.
 *
 * @author RollW
 */
public final class CorrelationId {
    private static final Normalizer<String> VALUE_NORMALIZER = Normalizer.of(String::trim)
            .validatedBy(
                    ValidationRules.nonBlank("Correlation ID")
                            .and(ValidationRules.noWhitespace("Correlation ID"))
                            .and(ValidationRules.noControlCharacters("Correlation ID"))
            );

    private final String value;

    private CorrelationId(String value) {
        this.value = value;
    }

    public static CorrelationId of(String value) {
        return new CorrelationId(VALUE_NORMALIZER.normalize(value));
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CorrelationId correlationId)) {
            return false;
        }
        return value.equals(correlationId.value);
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
