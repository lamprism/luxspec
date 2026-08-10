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

package com.lamprism.luxspec.validation;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Factory for reusable, non-annotated validation rules.
 *
 * <p>Rules validate values without changing them. Normalization such as
 * trimming remains an explicit decision of the value object that owns the
 * value.
 *
 * @author RollW
 */
public final class ValidationRules {
    private ValidationRules() {
    }

    /**
     * Creates a rule that rejects blank text.
     *
     * @param label the value label used in the failure detail
     * @return the non-blank rule
     */
    public static Validator<String> nonBlank(String label) {
        String nonBlankLabel = requireLabel(label);
        return Validator.of(
                value -> !value.isBlank(),
                nonBlankLabel + " must not be blank"
        );
    }

    /**
     * Creates a rule that rejects whitespace and space characters.
     *
     * @param label the value label used in the failure detail
     * @return the no-whitespace rule
     */
    public static Validator<String> noWhitespace(String label) {
        String nonBlankLabel = requireLabel(label);
        return Validator.of(
                ValidationRules::hasNoWhitespace,
                nonBlankLabel + " must not contain whitespace"
        );
    }

    /**
     * Creates a rule that rejects ISO control characters.
     *
     * @param label the value label used in the failure detail
     * @return the no-control-character rule
     */
    public static Validator<String> noControlCharacters(String label) {
        String nonBlankLabel = requireLabel(label);
        return Validator.of(
                ValidationRules::hasNoControlCharacters,
                nonBlankLabel + " must not contain control characters"
        );
    }

    /**
     * Creates a rule that limits a string to a maximum number of UTF-16 code units.
     *
     * @param label         the value label used in the failure detail
     * @param maximumLength the positive maximum length
     * @return the maximum-length rule
     */
    public static Validator<String> maxLength(String label, int maximumLength) {
        String nonBlankLabel = requireLabel(label);
        if (maximumLength < 1) {
            throw new IllegalArgumentException("maximumLength must be positive");
        }
        return Validator.of(
                value -> value.length() <= maximumLength,
                nonBlankLabel + " must not exceed " + maximumLength + " characters"
        );
    }

    /**
     * Creates a rule that accepts only values in a fixed non-empty set.
     *
     * @param label         the value label used in the failure detail
     * @param allowedValues the allowed values
     * @param <T>           the value type
     * @return the membership rule
     */
    public static <T> Validator<T> oneOf(String label, Set<? extends T> allowedValues) {
        String nonBlankLabel = requireLabel(label);
        Set<? extends T> values = Set.copyOf(Objects.requireNonNull(allowedValues, "allowedValues"));
        if (values.isEmpty()) {
            throw new IllegalArgumentException("allowedValues must not be empty");
        }
        return Validator.of(
                values::contains,
                nonBlankLabel + " must be an allowed value"
        );
    }

    /**
     * Creates a rule that applies an element rule to every list entry.
     *
     * @param elementValidator the element rule
     * @param <T>              the element type
     * @return the list element rule
     */
    public static <T> Validator<List<T>> elements(Validator<? super T> elementValidator) {
        Validator<? super T> nonNullValidator = Objects.requireNonNull(elementValidator, "elementValidator");
        return values -> {
            List<T> nonNullValues = Objects.requireNonNull(values, "value");
            for (T value : nonNullValues) {
                nonNullValidator.validate(value);
            }
        };
    }

    private static boolean hasNoWhitespace(String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isWhitespace(character) || Character.isSpaceChar(character)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasNoControlCharacters(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isISOControl(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private static String requireLabel(String label) {
        String nonNullLabel = Objects.requireNonNull(label, "label");
        if (nonNullLabel.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        return nonNullLabel;
    }
}
