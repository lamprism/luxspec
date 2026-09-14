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

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Normalizes one value into the canonical representation used by a domain boundary.
 *
 * <p>A normalizer may reject an input when no canonical representation exists. Validation can be
 * attached with {@link #validatedBy(Validator)} so callers receive the normalized and validated
 * value from one explicit operation.</p>
 *
 * @param <T> the normalized value type
 * @author RollW
 */
@FunctionalInterface
public interface Normalizer<T> {
    /**
     * Returns the canonical representation of one value.
     *
     * @param value the value to normalize
     * @return the normalized value
     */
    T normalize(T value);

    /**
     * Returns a normalizer that preserves every non-null value.
     *
     * @param <T> the value type
     * @return the identity normalizer
     */
    static <T> Normalizer<T> identity() {
        return value -> Objects.requireNonNull(value, "value");
    }

    /**
     * Creates a normalizer from a unary operation.
     *
     * @param operation the normalization operation
     * @param <T>       the value type
     * @return the operation-backed normalizer
     */
    static <T> Normalizer<T> of(UnaryOperator<T> operation) {
        UnaryOperator<T> nonNullOperation = Objects.requireNonNull(operation, "operation");
        return value -> Objects.requireNonNull(
                nonNullOperation.apply(Objects.requireNonNull(value, "value")),
                "normalized value"
        );
    }

    /**
     * Applies another normalizer after this normalizer.
     *
     * @param next the next normalization step
     * @return the composed normalizer
     */
    default Normalizer<T> andThen(Normalizer<T> next) {
        Normalizer<T> nonNullNext = Objects.requireNonNull(next, "next");
        return value -> nonNullNext.normalize(normalize(value));
    }

    /**
     * Validates the normalized result before returning it.
     *
     * @param validator the rule applied to the normalized value
     * @return the validating normalizer
     */
    default Normalizer<T> validatedBy(Validator<? super T> validator) {
        Validator<? super T> nonNullValidator = Objects.requireNonNull(validator, "validator");
        return value -> {
            T normalized = Objects.requireNonNull(normalize(value), "normalized value");
            nonNullValidator.validate(normalized);
            return normalized;
        };
    }
}
