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

package com.lamprism.luxspec.security.firewall.filtertable;

import java.util.Locale;
import java.util.Objects;

/**
 * Action applied when a filter-table entry matches.
 *
 * @author RollW
 */
public enum FilterMode {
    /**
     * Explicitly allow the matching request.
     */
    ALLOW,
    /**
     * Explicitly deny the matching request.
     */
    DENY;

    /**
     * Parses a case-insensitive mode name.
     *
     * @param value the mode name
     * @return the parsed mode
     */
    public static FilterMode fromString(String value) {
        String nonNullValue = Objects.requireNonNull(value, "value");
        String normalizedValue = nonNullValue.trim().toUpperCase(Locale.ROOT);
        for (FilterMode mode : values()) {
            if (mode.name().equals(normalizedValue)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown filter mode: " + value);
    }
}
