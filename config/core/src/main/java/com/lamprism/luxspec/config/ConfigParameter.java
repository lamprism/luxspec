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

import java.util.Objects;
import java.util.Set;

/**
 * Defines validation rules for one parameterized configuration path segment.
 *
 * @author RollW
 */
public final class ConfigParameter {
    private final String name;
    private final Set<String> allowedValues;

    /**
     * Creates a parameter rule.
     *
     * @param name          the parameter name
     * @param allowedValues the optional finite set of accepted values
     */
    public ConfigParameter(String name, Set<String> allowedValues) {
        this.name = requireName(name);
        this.allowedValues = Set.copyOf(allowedValues);
    }

    /**
     * Returns the parameter name.
     *
     * @return the parameter name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the finite values accepted by this parameter when the domain is constrained.
     *
     * <p>An empty set means that the parameter has no finite enumeration constraint.</p>
     *
     * @return the immutable allowed values, or an empty set for an unconstrained parameter
     */
    public Set<String> getAllowedValues() {
        return allowedValues;
    }

    /**
     * Validates one concrete parameter value.
     *
     * @param value the concrete path segment
     */
    public void validate(String value) {
        Objects.requireNonNull(value, "value");
        if (value.isEmpty() || value.indexOf('.') >= 0) {
            throw new IllegalArgumentException("Parameter value must be one non-empty path segment");
        }
        ConfigKey.of("parameter." + value);
        if (!allowedValues.isEmpty() && !allowedValues.contains(value)) {
            throw new IllegalArgumentException("Parameter value is not allowed: " + name);
        }
    }

    private static String requireName(String value) {
        Objects.requireNonNull(value, "name");
        ConfigKey.of("parameter." + value);
        return value;
    }
}
