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

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Defines a typed projection from selected query values to one result item.
 *
 * <p>The selection list is provider-neutral. The mapper receives immutable positional values in
 * that same order, including nullable selected values. The projected result item itself must not
 * be null. This allows in-memory and JPA executors to share the projection contract.</p>
 *
 * @param <R> the projected result type
 *
 * @author RollW
 */
public final class QueryProjection<R> {
    private final List<QueryValue<?>> values;
    private final Function<? super List<@Nullable Object>, ? extends R> mapper;

    private QueryProjection(
            List<? extends QueryValue<?>> values,
            Function<? super List<@Nullable Object>, ? extends R> mapper
    ) {
        List<QueryValue<?>> copiedValues = new ArrayList<>(Objects.requireNonNull(values, "values"));
        if (copiedValues.isEmpty()) {
            throw new IllegalArgumentException("A query projection requires at least one value");
        }
        for (QueryValue<?> value : copiedValues) {
            Objects.requireNonNull(value, "value");
        }
        this.values = List.copyOf(copiedValues);
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    /**
     * Creates a projection containing one non-null typed value.
     *
     * @param value the selected value
     * @param <T>   the value type
     * @return the single-value projection
     */
    public static <T> QueryProjection<T> single(QueryValue<T> value) {
        QueryValue<T> nonNullValue = Objects.requireNonNull(value, "value");
        return new QueryProjection<>(List.of(nonNullValue), selected -> {
            @SuppressWarnings("unchecked")
            T result = (T) selected.get(0);
            return Objects.requireNonNull(result, "projection result");
        });
    }

    /**
     * Creates a projection that represents a nullable value as an optional item.
     *
     * @param value the selected value
     * @param <T>   the value type
     * @return the optional-value projection
     */
    public static <T> QueryProjection<Optional<T>> optional(QueryValue<T> value) {
        QueryValue<T> nonNullValue = Objects.requireNonNull(value, "value");
        return new QueryProjection<>(List.of(nonNullValue),
                selected -> Optional.ofNullable(nonNullValue.getValueType().cast(selected.get(0))));
    }

    /**
     * Creates a projection with an application-owned result mapper.
     *
     * @param values the selected values
     * @param mapper the positional result mapper, which may receive null selected values
     * @param <R>    the projected result type
     * @return the immutable projection
     */
    public static <R> QueryProjection<R> of(
            List<? extends QueryValue<?>> values,
            Function<? super List<@Nullable Object>, ? extends R> mapper
    ) {
        return new QueryProjection<>(values, mapper);
    }

    /**
     * Returns the selected provider-neutral values.
     *
     * @return immutable selected values
     */
    public List<QueryValue<?>> getValues() {
        return values;
    }

    /**
     * Maps provider results to the projection result type.
     *
     * @param selected selected values in projection order
     * @return the projected item
     */
    public R project(List<?> selected) {
        List<?> nonNullSelected = Objects.requireNonNull(selected, "selected");
        if (nonNullSelected.size() != values.size()) {
            throw new IllegalArgumentException("Projection value count does not match the projection");
        }
        List<@Nullable Object> copied = new ArrayList<>(nonNullSelected.size());
        for (Object value : nonNullSelected) {
            copied.add(value);
        }
        return Objects.requireNonNull(mapper.apply(Collections.unmodifiableList(copied)), "projection result");
    }
}
