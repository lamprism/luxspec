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

package com.lamprism.luxspec.audit;

import com.lamprism.luxspec.data.query.QueryField;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable typed custom audit field assignments.
 *
 * <p>Fields use {@link QueryField} names and value types. Values must be
 * non-null, and assigning the same field name more than once is rejected. The
 * set is structurally immutable; application-owned field values are not deeply
 * copied.
 *
 * @author RollW
 */
public final class AuditFieldSet {
    private static final AuditFieldSet EMPTY = new AuditFieldSet(Map.of());
    private final Map<QueryField<?>, Object> values;

    private AuditFieldSet(Map<QueryField<?>, Object> values) {
        this.values = Map.copyOf(values);
    }

    public static AuditFieldSet empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<QueryField<?>, Object> values() {
        return values;
    }

    public <T> Optional<T> get(QueryField<T> field) {
        QueryField<T> nonNullField = Objects.requireNonNull(field, "field");
        Object value = values.get(nonNullField);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(nonNullField.getValueType().cast(value));
    }

    /**
     * Builds an immutable field set.
     */
    public static final class Builder {
        private final Map<QueryField<?>, Object> values = new LinkedHashMap<>();
        private final Map<String, QueryField<?>> names = new LinkedHashMap<>();

        private Builder() {
        }

        public <T> Builder put(QueryField<T> field, T value) {
            QueryField<T> nonNullField = Objects.requireNonNull(field, "field");
            Objects.requireNonNull(value, "value");
            if (!nonNullField.getValueType().isInstance(value)) {
                throw new IllegalArgumentException("Audit field value does not match field type: " + nonNullField.getName());
            }
            if (names.putIfAbsent(nonNullField.getName(), nonNullField) != null) {
                throw new IllegalArgumentException("Audit field is assigned more than once: " + nonNullField.getName());
            }
            values.put(nonNullField, value);
            return this;
        }

        public AuditFieldSet build() {
            return values.isEmpty() ? EMPTY : new AuditFieldSet(values);
        }
    }
}
