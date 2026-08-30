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
import java.util.function.Predicate;

/**
 * One composable fail-fast validation contract.
 *
 * <p>Validators reject null values, apply one or more explicit rules, and
 * report the first failure. This API deliberately does not use annotations,
 * reflection, object graph traversal, validation groups, or implicit
 * normalization.
 *
 * @param <T> the value type
 * @author RollW
 */
@FunctionalInterface
public interface Validator<T> {
    /**
     * Validates one value.
     *
     * @param value the value to validate
     * @throws ValidationException when the value violates the rule
     */
    void validate(T value);

    /**
     * Returns a validator that accepts every non-null value.
     *
     * @param <T> the value type
     * @return the accepting validator
     */
    static <T> Validator<T> none() {
        return value -> Objects.requireNonNull(value, "value");
    }

    /**
     * Creates a validator from a predicate and a value-free failure detail.
     *
     * @param predicate the value predicate
     * @param detail    the failure detail
     * @param <T>       the value type
     * @return the predicate validator
     */
    static <T> Validator<T> of(Predicate<? super T> predicate, String detail) {
        Predicate<? super T> nonNullPredicate = Objects.requireNonNull(predicate, "predicate");
        String nonBlankDetail = requireDetail(detail);
        return value -> {
            T nonNullValue = Objects.requireNonNull(value, "value");
            if (!nonNullPredicate.test(nonNullValue)) {
                throw new ValidationException(nonBlankDetail);
            }
        };
    }

    /**
     * Combines this validator with another validator in declaration order.
     *
     * @param other the additional validator
     * @return the composed validator
     */
    default Validator<T> and(Validator<? super T> other) {
        Validator<? super T> nonNullOther = Objects.requireNonNull(other, "other");
        return value -> {
            validate(value);
            nonNullOther.validate(value);
        };
    }

    private static String requireDetail(String detail) {
        String nonNullDetail = Objects.requireNonNull(detail, "detail");
        if (nonNullDetail.isBlank()) {
            throw new IllegalArgumentException("detail must not be blank");
        }
        return nonNullDetail;
    }
}
