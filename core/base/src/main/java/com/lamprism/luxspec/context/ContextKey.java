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

package com.lamprism.luxspec.context;

import java.util.Objects;

/**
 * Identifies one typed immutable ExecutionContext element.
 *
 * @param <T> the element type
 * @author RollW
 */
public final class ContextKey<T> {
    private final String name;
    private final Class<T> valueType;

    private ContextKey(String name, Class<T> valueType) {
        this.name = name;
        this.valueType = valueType;
    }

    /**
     * Creates a typed immutable context-key identity.
     *
     * @param name      the non-blank key name
     * @param valueType the context value type
     * @param <T>       the context value type
     * @return the immutable context key
     */
    public static <T> ContextKey<T> of(String name, Class<T> valueType) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return new ContextKey<>(name, Objects.requireNonNull(valueType, "valueType"));
    }

    /**
     * Returns the stable key name.
     *
     * @return the key name
     */
    public String getName() {
        return name;
    }

    Class<T> getValueType() {
        return valueType;
    }
}
