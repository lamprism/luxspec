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

package com.lamprism.luxspec.config;

import com.lamprism.luxspec.config.value.ConfigValueValidationException;
import com.lamprism.luxspec.validation.Validator;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Validates a decoded typed configuration value against one definition's domain rules.
 *
 * @param <T> the typed value
 * @author RollW
 */
@FunctionalInterface
public interface ConfigValueValidator<T> extends Validator<T> {
    /**
     * Validates one non-null value.
     *
     * @param value the typed value
     * @throws ConfigValueValidationException when the value violates the rule
     */
    @Override
    void validate(T value);

    /**
     * Returns a validator that accepts every non-null value.
     *
     * @param <T> the typed value
     * @return the accepting validator
     */
    static <T> ConfigValueValidator<T> none() {
        return value -> Objects.requireNonNull(value, "value");
    }

    /**
     * Creates a validator from a predicate and a value-free failure detail.
     *
     * @param predicate the typed value predicate
     * @param detail    the value-free failure detail
     * @param <T>       the typed value
     * @return the predicate validator
     */
    static <T> ConfigValueValidator<T> of(Predicate<? super T> predicate, String detail) {
        Predicate<? super T> nonNullPredicate = Objects.requireNonNull(predicate, "predicate");
        String nonBlankDetail = requireDetail(detail);
        return value -> {
            T nonNullValue = Objects.requireNonNull(value, "value");
            if (!nonNullPredicate.test(nonNullValue)) {
                throw new ConfigValueValidationException(nonBlankDetail);
            }
        };
    }

    /**
     * Composes this validator with another validator.
     *
     * @param other the additional validator
     * @return a validator that applies both rules in order
     */
    @Override
    default ConfigValueValidator<T> and(Validator<? super T> other) {
        Validator<? super T> nonNullOther = Objects.requireNonNull(other, "other");
        return value -> {
            validate(value);
            nonNullOther.validate(value);
        };
    }

    private static String requireDetail(String detail) {
        String nonBlankDetail = Objects.requireNonNull(detail, "detail");
        if (nonBlankDetail.isBlank()) {
            throw new IllegalArgumentException("detail must not be blank");
        }
        return nonBlankDetail;
    }
}
