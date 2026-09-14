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

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a fixed or parameterized configuration key expression.
 *
 * <p>A complete key contains only concrete path segments. A parameterized key expression may be
 * bound to a complete key or matched against one to recover validated arguments.</p>
 *
 * <p>Key expressions use structural equality: both the serialized expression and its parameter
 * declarations must match.</p>
 *
 * @author RollW
 */
public final class ConfigKey {
    private final String value;
    private final List<String> segments;
    private final Map<String, ConfigParameter> parameters;

    private ConfigKey(
            String value,
            List<String> segments,
            Map<String, ConfigParameter> parameters
    ) {
        this.value = value;
        this.segments = List.copyOf(segments);
        this.parameters = Map.copyOf(parameters);
    }

    /**
     * Creates a validated complete configuration key.
     *
     * @param value the dotted key value without parameters
     * @return the complete key
     */
    public static ConfigKey of(String value) {
        String nonNullValue = Objects.requireNonNull(value, "value");
        validateCompleteValue(nonNullValue);
        return new ConfigKey(nonNullValue, split(nonNullValue), Map.of());
    }

    /**
     * Creates a key expression that may contain complete parameter segments such as
     * {@code {tenant}}.
     *
     * @param value      the key expression
     * @param parameters declarations for every parameter segment
     * @return the fixed or parameterized key expression
     */
    public static ConfigKey template(String value, List<ConfigParameter> parameters) {
        List<String> segments = splitTemplate(Objects.requireNonNull(value, "value"));
        Map<String, ConfigParameter> indexedParameters = index(parameters);
        validateSegments(segments, indexedParameters);
        return new ConfigKey(String.join(".", segments), segments, indexedParameters);
    }

    /**
     * Returns the serialized key expression or complete key value.
     *
     * @return the dotted key value
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the immutable literal and parameter segments.
     *
     * @return the key segments
     */
    public List<String> getSegments() {
        return segments;
    }

    /**
     * Returns the declared parameter names and rules.
     *
     * @return immutable parameter declarations
     */
    public Map<String, ConfigParameter> getParameters() {
        return parameters;
    }

    /**
     * Reports whether this key expression contains parameters.
     *
     * @return {@code true} when the key must be bound before use by a Source
     */
    public boolean isParameterized() {
        return !parameters.isEmpty();
    }

    /**
     * Returns the number of literal segments used for catalog specificity.
     *
     * @return the literal segment count
     */
    public int fixedSegmentCount() {
        int count = 0;
        for (String segment : segments) {
            if (!isParameter(segment)) {
                count++;
            }
        }
        return count;
    }

    ConfigKey bind(Map<String, String> arguments) {
        Map<String, String> values = Map.copyOf(Objects.requireNonNull(arguments, "arguments"));
        if (!values.keySet().equals(parameters.keySet())) {
            throw new IllegalArgumentException("Configuration key arguments do not match parameters");
        }
        List<String> boundSegments = new ArrayList<>();
        for (String segment : segments) {
            ConfigParameter parameter = parameter(segment);
            if (parameter == null) {
                boundSegments.add(segment);
                continue;
            }
            String value = Objects.requireNonNull(values.get(parameter.getName()), "argument value");
            parameter.validate(value);
            boundSegments.add(value);
        }
        return ConfigKey.of(String.join(".", boundSegments));
    }

    @Nullable
    Map<String, String> match(ConfigKey key) {
        ConfigKey nonNullKey = Objects.requireNonNull(key, "key");
        if (nonNullKey.isParameterized()) {
            return null;
        }
        List<String> keySegments = split(nonNullKey.getValue());
        if (keySegments.size() != segments.size()) {
            return null;
        }
        Map<String, String> arguments = new LinkedHashMap<>();
        for (int index = 0; index < segments.size(); index++) {
            String expressionSegment = segments.get(index);
            String keySegment = keySegments.get(index);
            ConfigParameter parameter = parameter(expressionSegment);
            if (parameter == null) {
                if (!expressionSegment.equals(keySegment)) {
                    return null;
                }
                continue;
            }
            try {
                parameter.validate(keySegment);
            } catch (IllegalArgumentException exception) {
                return null;
            }
            arguments.put(parameter.getName(), keySegment);
        }
        return Map.copyOf(arguments);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ConfigKey key
                && value.equals(key.value)
                && parameters.equals(key.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, parameters);
    }

    @Override
    public String toString() {
        return value;
    }

    @Nullable
    private ConfigParameter parameter(String segment) {
        if (isParameter(segment)) {
            return parameters.get(segment.substring(1, segment.length() - 1));
        }
        return null;
    }

    private static List<String> splitTemplate(String value) {
        List<String> segments = split(value);
        for (String segment : segments) {
            if (segment.isEmpty()) {
                throw new IllegalArgumentException("Configuration key contains an empty segment");
            }
        }
        return segments;
    }

    private static List<String> split(String value) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Configuration key must not be empty");
        }
        List<String> segments = new ArrayList<>();
        int segmentStart = 0;
        for (int index = 0; index <= value.length(); index++) {
            if (index != value.length() && value.charAt(index) != '.') {
                continue;
            }
            segments.add(value.substring(segmentStart, index));
            segmentStart = index + 1;
        }
        return List.copyOf(segments);
    }

    private static Map<String, ConfigParameter> index(List<ConfigParameter> parameters) {
        Map<String, ConfigParameter> result = new LinkedHashMap<>();
        for (ConfigParameter parameter : Objects.requireNonNull(parameters, "parameters")) {
            ConfigParameter nonNullParameter = Objects.requireNonNull(parameter, "parameter");
            if (result.put(nonNullParameter.getName(), nonNullParameter) != null) {
                throw new IllegalArgumentException("Duplicate configuration key parameter");
            }
        }
        return result;
    }

    private static void validateSegments(
            List<String> segments,
            Map<String, ConfigParameter> parameters
    ) {
        Set<String> used = new HashSet<>();
        for (String segment : segments) {
            if (isParameter(segment)) {
                String name = segment.substring(1, segment.length() - 1);
                if (!parameters.containsKey(name) || !used.add(name)) {
                    throw new IllegalArgumentException("Configuration key parameter declaration is invalid");
                }
                continue;
            }
            if (segment.indexOf('{') >= 0 || segment.indexOf('}') >= 0) {
                throw new IllegalArgumentException("Configuration parameters must occupy complete segments");
            }
            validateCompleteValue("key." + segment);
        }
        if (!used.equals(parameters.keySet())) {
            throw new IllegalArgumentException("Unused configuration key parameter declaration");
        }
    }

    private static void validateCompleteValue(String value) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException("value must not be empty");
        }
        boolean segmentStart = true;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '.') {
                if (segmentStart) {
                    throw new IllegalArgumentException("value contains an empty segment");
                }
                segmentStart = true;
                continue;
            }
            if (!isAllowed(character)) {
                throw new IllegalArgumentException("value contains an unsupported character");
            }
            segmentStart = false;
        }
        if (segmentStart) {
            throw new IllegalArgumentException("value must not end with a separator");
        }
    }

    private static boolean isAllowed(char character) {
        return character >= 'a' && character <= 'z'
                || character >= 'A' && character <= 'Z'
                || character >= '0' && character <= '9'
                || character == '-'
                || character == '_';
    }

    private static boolean isParameter(String segment) {
        return segment.length() > 2
                && segment.charAt(0) == '{'
                && segment.charAt(segment.length() - 1) == '}';
    }
}
