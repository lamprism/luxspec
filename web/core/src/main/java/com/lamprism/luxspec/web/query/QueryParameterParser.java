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

package com.lamprism.luxspec.web.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * Converts one already decoded HTTP parameter value into a declared query value type.
 *
 * <p>Applications provide parsers for domain types explicitly. The standard helpers cover
 * common JDK scalar values and never instantiate an arbitrary class from request data.</p>
 *
 * @param <T> the parsed value type
 * @author RollW
 */
@FunctionalInterface
public interface QueryParameterParser<T> {
    /**
     * Parses one parameter value.
     *
     * @param raw the decoded raw value
     * @return the non-null typed value
     */
    T parse(String raw);

    /**
     * Formats one typed value using the same stable textual representation used by query values.
     *
     * <p>The default representation is suitable for the standard scalar types and can be
     * replaced by a custom parser when a domain type needs a canonical external form.</p>
     *
     * @param value the typed value
     * @return the non-null encoded value
     */
    default String format(T value) {
        return String.valueOf(Objects.requireNonNull(value, "value"));
    }

    /**
     * Creates a parser from an explicit conversion function.
     *
     * @param converter the conversion function
     * @param <T>       the parsed type
     * @return the parser
     */
    static <T> QueryParameterParser<T> of(Function<? super String, ? extends T> converter) {
        Function<? super String, ? extends T> nonNullConverter = Objects.requireNonNull(converter, "converter");
        return raw -> nonNullConverter.apply(Objects.requireNonNull(raw, "raw"));
    }

    /**
     * Creates a parser with an explicit canonical formatter.
     *
     * @param converter the textual decoder
     * @param formatter the typed value formatter
     * @param <T>       the parsed value type
     * @return the parser and formatter pair
     */
    static <T> QueryParameterParser<T> of(
            Function<? super String, ? extends T> converter,
            Function<? super T, ? extends String> formatter
    ) {
        Function<? super String, ? extends T> nonNullConverter = Objects.requireNonNull(converter, "converter");
        Function<? super T, ? extends String> nonNullFormatter = Objects.requireNonNull(formatter, "formatter");
        return new QueryParameterParser<>() {
            @Override
            public T parse(String raw) {
                return nonNullConverter.apply(Objects.requireNonNull(raw, "raw"));
            }

            @Override
            public String format(T value) {
                return Objects.requireNonNull(
                        nonNullFormatter.apply(Objects.requireNonNull(value, "value")),
                        "formatted value"
                );
            }
        };
    }

    /**
     * Returns a parser that preserves strings.
     *
     * @return the string parser
     */
    static QueryParameterParser<String> string() {
        return raw -> Objects.requireNonNull(raw, "raw");
    }

    /**
     * Returns a parser for 32-bit signed integers.
     *
     * @return the integer parser
     */
    static QueryParameterParser<Integer> integer() {
        return raw -> Integer.valueOf(Objects.requireNonNull(raw, "raw"));
    }

    /**
     * Returns a parser for 64-bit signed integers.
     *
     * @return the long parser
     */
    static QueryParameterParser<Long> longValue() {
        return raw -> Long.valueOf(Objects.requireNonNull(raw, "raw"));
    }

    /**
     * Returns a strict parser for boolean values.
     *
     * @return the boolean parser
     */
    static QueryParameterParser<Boolean> booleanValue() {
        return raw -> {
            String nonNullRaw = Objects.requireNonNull(raw, "raw");
            if ("true".equalsIgnoreCase(nonNullRaw)) {
                return true;
            }
            if ("false".equalsIgnoreCase(nonNullRaw)) {
                return false;
            }
            throw new IllegalArgumentException("Invalid boolean value");
        };
    }

    /**
     * Returns a parser for arbitrary-precision decimal values.
     *
     * @return the decimal parser
     */
    static QueryParameterParser<BigDecimal> decimal() {
        return raw -> new BigDecimal(Objects.requireNonNull(raw, "raw"));
    }

    /**
     * Returns an ISO-8601 instant parser.
     *
     * @return the instant parser
     */
    static QueryParameterParser<Instant> instant() {
        return raw -> Instant.parse(Objects.requireNonNull(raw, "raw"));
    }

    /**
     * Returns a canonical UUID parser.
     *
     * @return the UUID parser
     */
    static QueryParameterParser<UUID> uuid() {
        return raw -> UUID.fromString(Objects.requireNonNull(raw, "raw"));
    }

    /**
     * Returns a parser for the exact enum constant name.
     *
     * @param enumType the application enum type
     * @param <E>      the enum type
     * @return the enum parser
     */
    static <E extends Enum<E>> QueryParameterParser<E> enumValue(Class<E> enumType) {
        Class<E> nonNullEnumType = Objects.requireNonNull(enumType, "enumType");
        return QueryParameterParser.of(
                raw -> Enum.valueOf(nonNullEnumType, Objects.requireNonNull(raw, "raw")),
                Enum::name
        );
    }
}
