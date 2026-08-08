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

package com.lamprism.luxspec.naming;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Common name case formats and conversions between them.
 *
 * @author RollW
 */
public enum CaseFormat {
    /**
     * Lower-case words separated by hyphens.
     */
    LOWER_HYPHEN('-', false),
    /**
     * Lower-case words separated by underscores.
     */
    LOWER_UNDERSCORE('_', false),
    /**
     * A lower-case first word followed by upper-case word initials.
     */
    LOWER_CAMEL('\0', true),
    /**
     * Upper-case word initials for every word.
     */
    UPPER_CAMEL('\0', true),
    /**
     * Upper-case words separated by underscores.
     */
    UPPER_UNDERSCORE('_', false);

    private final char separator;
    private final boolean camelCase;

    CaseFormat(char separator, boolean camelCase) {
        this.separator = separator;
        this.camelCase = camelCase;
    }

    /**
     * Converts one name to another case format.
     *
     * @param targetFormat the target case format
     * @param value        the name to convert
     * @return the converted name
     */
    public String to(CaseFormat targetFormat, String value) {
        CaseFormat nonNullTargetFormat = Objects.requireNonNull(targetFormat, "targetFormat");
        String nonNullValue = requireValue(value);
        if (this == nonNullTargetFormat || nonNullValue.isEmpty()) {
            return nonNullValue;
        }
        return nonNullTargetFormat.join(split(nonNullValue));
    }

    /**
     * Creates a reusable converter to another case format.
     *
     * @param targetFormat the target case format
     * @return a name converter for the target format
     */
    public NameConverter to(CaseFormat targetFormat) {
        CaseFormat nonNullTargetFormat = Objects.requireNonNull(targetFormat, "targetFormat");
        return value -> to(nonNullTargetFormat, value);
    }

    private List<String> split(String value) {
        if (camelCase) {
            return splitCamelCase(value);
        }
        return splitSeparated(value);
    }

    private List<String> splitSeparated(String value) {
        List<String> words = new ArrayList<>();
        int wordStart = 0;
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) != separator) {
                continue;
            }
            if (wordStart == index) {
                throw new IllegalArgumentException("Name contains an empty case segment");
            }
            words.add(value.substring(wordStart, index));
            wordStart = index + 1;
        }
        if (wordStart == value.length()) {
            throw new IllegalArgumentException("Name must not end with a case separator");
        }
        words.add(value.substring(wordStart));
        return List.copyOf(words);
    }

    private List<String> splitCamelCase(String value) {
        List<String> words = new ArrayList<>();
        int wordStart = 0;
        for (int index = 1; index < value.length(); index++) {
            if (!isWordBoundary(value, index)) {
                continue;
            }
            words.add(value.substring(wordStart, index));
            wordStart = index;
        }
        words.add(value.substring(wordStart));
        return List.copyOf(words);
    }

    private boolean isWordBoundary(String value, int index) {
        char current = value.charAt(index);
        if (!Character.isUpperCase(current)) {
            return false;
        }
        char previous = value.charAt(index - 1);
        if (Character.isLowerCase(previous) || Character.isDigit(previous)) {
            return true;
        }
        return Character.isUpperCase(previous)
                && index + 1 < value.length()
                && Character.isLowerCase(value.charAt(index + 1));
    }

    private String join(List<String> words) {
        if (this == LOWER_CAMEL) {
            return lowerCamel(words);
        }
        if (this == UPPER_CAMEL) {
            return upperCamel(words);
        }
        String separatorValue = String.valueOf(separator);
        StringJoiner joiner = new StringJoiner(separatorValue);
        for (String word : words) {
            joiner.add(this == UPPER_UNDERSCORE ? word.toUpperCase(Locale.ROOT) : word.toLowerCase(Locale.ROOT));
        }
        return joiner.toString();
    }

    private static String lowerCamel(List<String> words) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < words.size(); index++) {
            String word = words.get(index);
            String normalized = index == 0
                    ? word.toLowerCase(Locale.ROOT)
                    : upperFirst(word.toLowerCase(Locale.ROOT));
            result.append(normalized);
        }
        return result.toString();
    }

    private static String upperCamel(List<String> words) {
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            result.append(upperFirst(word.toLowerCase(Locale.ROOT)));
        }
        return result.toString();
    }

    private static String upperFirst(String value) {
        if (value.isEmpty()) {
            return value;
        }
        StringBuilder result = new StringBuilder(value);
        result.setCharAt(0, Character.toUpperCase(result.charAt(0)));
        return result.toString();
    }

    private static String requireValue(String value) {
        String nonNullValue = Objects.requireNonNull(value, "value");
        for (int index = 0; index < nonNullValue.length(); index++) {
            char character = nonNullValue.charAt(index);
            if (Character.isWhitespace(character)
                    || Character.isSpaceChar(character)
                    || Character.isISOControl(character)) {
                throw new IllegalArgumentException("Name must not contain whitespace or control characters");
            }
        }
        return nonNullValue;
    }
}
