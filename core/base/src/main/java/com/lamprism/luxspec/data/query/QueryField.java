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

package com.lamprism.luxspec.data.query;

import com.lamprism.luxspec.validation.ValidationRules;
import com.lamprism.luxspec.validation.Validator;

import java.util.Objects;

/**
 * Identifies one domain-owned query field and its Java value type.
 * <p>
 * The name is an opaque domain key. Persistence adapters may apply their own
 * naming strategy when they map it to a provider-specific attribute.
 * Query field names must be non-empty and must not contain whitespace, space
 * characters, or ISO control characters. Names are not trimmed. The value type
 * must be a non-primitive Java type.
 *
 * @param <T> the field value type
 * @author RollW
 */
public final class QueryField<T> {
    private static final Validator<String> NAME_VALIDATOR = ValidationRules
            .nonBlank("Query field name")
            .and(ValidationRules.noWhitespace("Query field name"))
            .and(ValidationRules.noControlCharacters("Query field name"));

    private final String name;
    private final Class<T> valueType;

    private QueryField(String name, Class<T> valueType) {
        this.name = name;
        this.valueType = valueType;
    }

    /**
     * Creates a typed field identity.
     *
     * @param name      the stable domain field name
     * @param valueType the non-primitive Java value type
     * @param <T>       the field value type
     * @return the immutable field identity
     */
    public static <T> QueryField<T> of(String name, Class<T> valueType) {
        Class<T> nonNullValueType = Objects.requireNonNull(valueType, "valueType");
        if (nonNullValueType.isPrimitive()) {
            throw new IllegalArgumentException("Query field value types must use boxed classes");
        }
        return new QueryField<>(requireName(name), nonNullValueType);
    }

    /**
     * Returns the stable domain field name.
     *
     * @return the field name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the Java type accepted by this field.
     *
     * @return the field value type
     */
    public Class<T> getValueType() {
        return valueType;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof QueryField<?> queryField)) {
            return false;
        }
        return name.equals(queryField.name) && valueType.equals(queryField.valueType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, valueType);
    }

    @Override
    public String toString() {
        return name;
    }

    void requireValue(T value) {
        T nonNullValue = Objects.requireNonNull(value, "value");
        if (!valueType.isInstance(nonNullValue)) {
            throw new IllegalArgumentException("Query value does not match field type: " + name);
        }
    }

    private static String requireName(String name) {
        String nonNullName = Objects.requireNonNull(name, "name");
        NAME_VALIDATOR.validate(nonNullName);
        return nonNullName;
    }
}
