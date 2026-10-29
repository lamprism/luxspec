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

package com.lamprism.luxspec.config.source;

import java.util.Objects;

/**
 * Identifies one configured configuration source instance.
 *
 * @author RollW
 */
public final class ConfigSourceId {
    private final String value;

    private ConfigSourceId(String value) {
        this.value = value;
    }

    /**
     * Creates a non-blank source instance identifier.
     *
     * @param value the stable source identifier
     * @return the source identifier
     */
    public static ConfigSourceId of(String value) {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
        return new ConfigSourceId(value);
    }

    /**
     * Returns the serialized source identifier.
     *
     * @return the source identifier value
     */
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ConfigSourceId identifier && value.equals(identifier.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
