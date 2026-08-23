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
import com.lamprism.luxspec.validation.Validator;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

/**
 * Typed operation attribute declaration with stable formatting rules.
 *
 * <p>Attribute names are trimmed and reject empty values, whitespace, space
 * characters, and ISO control characters. Values must match the declared type,
 * and the explicit formatter must return a non-empty string. Core does not
 * infer a formatter or silently call {@code toString()}.
 *
 * @author RollW
 */
public final class ObservationAttributeSpec<T> {
    private static final Validator<String> NAME_VALIDATOR = ValidationRules
            .nonBlank("Observation attribute name")
            .and(ValidationRules.noWhitespace("Observation attribute name"))
            .and(ValidationRules.noControlCharacters("Observation attribute name"));

    private final String name;
    private final Class<T> valueType;
    private final ObservationAttributeClassification classification;
    private final Function<? super T, @Nullable String> formatter;

    private ObservationAttributeSpec(
            String name,
            Class<T> valueType,
            ObservationAttributeClassification classification,
            Function<? super T, @Nullable String> formatter
    ) {
        this.name = requireName(name);
        this.valueType = Objects.requireNonNull(valueType, "valueType");
        this.classification = Objects.requireNonNull(classification, "classification");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
    }

    public static <T> ObservationAttributeSpec<T> of(
            String name,
            Class<T> valueType,
            ObservationAttributeClassification classification,
            Function<? super T, @Nullable String> formatter
    ) {
        return new ObservationAttributeSpec<>(name, valueType, classification, formatter);
    }

    public static ObservationAttributeSpec<String> string(
            String name,
            ObservationAttributeClassification classification
    ) {
        return of(name, String.class, classification, Function.identity());
    }

    public static <T extends Enum<T>> ObservationAttributeSpec<T> enumValue(
            String name,
            Class<T> valueType,
            ObservationAttributeClassification classification
    ) {
        return of(name, valueType, classification, Enum::name);
    }

    public String name() {
        return name;
    }

    public Class<T> valueType() {
        return valueType;
    }

    public ObservationAttributeClassification classification() {
        return classification;
    }

    public String formatValue(T value) {
        Objects.requireNonNull(value, "value");
        if (!valueType.isInstance(value)) {
            throw new IllegalArgumentException("Observation attribute value does not match type: " + name);
        }
        String formatted = formatter.apply(value);
        if (formatted == null || formatted.isEmpty()) {
            throw new IllegalArgumentException("Observation attribute formatter returned an empty value: " + name);
        }
        return formatted;
    }

    String format(T value) {
        return formatValue(value);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ObservationAttributeSpec<?> spec)) {
            return false;
        }
        return name.equals(spec.name)
                && valueType.equals(spec.valueType)
                && classification == spec.classification;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, valueType, classification);
    }

    @Override
    public String toString() {
        return name;
    }

    private static String requireName(String value) {
        String normalized = Objects.requireNonNull(value, "name").trim();
        NAME_VALIDATOR.validate(normalized);
        return normalized;
    }
}
