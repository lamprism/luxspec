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

package com.lamprism.luxspec.console;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;

/**
 * Standard dependency-free value parsers for command specifications.
 *
 * @author RollW
 */
public final class ValueParsers {
    private ValueParsers() {
    }

    /**
     * @return a parser that preserves the token exactly
     */
    public static ValueParser<String> string() {
        return ValueParsers::parseString;
    }

    /**
     * @return a parser for signed base-ten integers
     */
    public static ValueParser<Integer> integer() {
        return ValueParsers::parseInteger;
    }

    /**
     * @return a parser for signed base-ten long values
     */
    public static ValueParser<Long> longValue() {
        return ValueParsers::parseLong;
    }

    /**
     * @return a parser for decimal double values
     */
    public static ValueParser<Double> doubleValue() {
        return ValueParsers::parseDouble;
    }

    /**
     * @return a parser that accepts only true or false, ignoring case
     */
    public static ValueParser<Boolean> booleanValue() {
        return ValueParsers::parseBoolean;
    }

    /**
     * @return a parser for normalized filesystem paths
     */
    public static ValueParser<Path> path() {
        return ValueParsers::parsePath;
    }

    /**
     * Creates a parser from a pure conversion function.
     *
     * @param converter the conversion function
     * @param <T>       parsed value type
     * @return a parser that translates conversion failures into value failures
     */
    public static <T> ValueParser<T> of(Function<String, T> converter) {
        Objects.requireNonNull(converter, "converter");
        return token -> {
            try {
                T value = converter.apply(token);
                if (value == null) {
                    throw new ValueParseException(token, "The value parser returned null");
                }
                return value;
            } catch (ValueParseException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                String message = exception.getMessage();
                if (message == null || message.isBlank()) {
                    message = "The value is invalid";
                }
                throw new ValueParseException(token, message, exception);
            }
        };
    }

    /**
     * Creates a case-sensitive enum parser.
     *
     * @param enumType enum type
     * @param <E>      enum type
     * @return the enum parser
     */
    public static <E extends Enum<E>> ValueParser<E> enumValue(Class<E> enumType) {
        Objects.requireNonNull(enumType, "enumType");
        return token -> {
            try {
                return Enum.valueOf(enumType, token);
            } catch (IllegalArgumentException exception) {
                throw new ValueParseException(token,
                        "Expected one of " + String.join(", ", enumConstants(enumType)), exception);
            }
        };
    }

    private static String parseString(String token) {
        return Objects.requireNonNull(token, "token");
    }

    private static Integer parseInteger(String token) throws ValueParseException {
        try {
            return Integer.valueOf(token);
        } catch (NumberFormatException exception) {
            throw new ValueParseException(token, "Expected an integer", exception);
        }
    }

    private static Long parseLong(String token) throws ValueParseException {
        try {
            return Long.valueOf(token);
        } catch (NumberFormatException exception) {
            throw new ValueParseException(token, "Expected a long integer", exception);
        }
    }

    private static Double parseDouble(String token) throws ValueParseException {
        try {
            return Double.valueOf(token);
        } catch (NumberFormatException exception) {
            throw new ValueParseException(token, "Expected a decimal number", exception);
        }
    }

    private static Boolean parseBoolean(String token) throws ValueParseException {
        if ("true".equalsIgnoreCase(token)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(token)) {
            return Boolean.FALSE;
        }
        throw new ValueParseException(token, "Expected true or false");
    }

    private static Path parsePath(String token) throws ValueParseException {
        try {
            return Path.of(token);
        } catch (RuntimeException exception) {
            throw new ValueParseException(token, "Expected a filesystem path", exception);
        }
    }

    private static <E extends Enum<E>> String[] enumConstants(Class<E> enumType) {
        E[] constants = enumType.getEnumConstants();
        String[] names = new String[constants.length];
        for (int index = 0; index < constants.length; index++) {
            names[index] = constants[index].name();
        }
        return names;
    }
}
