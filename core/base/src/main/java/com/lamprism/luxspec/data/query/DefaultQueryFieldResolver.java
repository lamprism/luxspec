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

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

class DefaultQueryFieldResolver<T> implements QueryFieldResolver<T> {
    private final Map<QueryField<?>, FieldAccessor<T, ?>> accessors;

    DefaultQueryFieldResolver(
            Map<QueryField<?>, FieldAccessor<T, ?>> accessors
    ) {
        this.accessors = Objects.requireNonNull(accessors, "accessors");
    }

    @Override
    public <V> @Nullable V resolve(T candidate, QueryField<V> field) {
        return accessor(field).resolve(candidate);
    }

    @Override
    public <V> int compare(T left, T right, QueryField<V> field) {
        return accessor(field).compare(left, right);
    }

    @SuppressWarnings("unchecked")
    private <V> FieldAccessor<T, V> accessor(QueryField<V> field) {
        QueryField<V> nonNullField = Objects.requireNonNull(field, "field");
        FieldAccessor<T, ?> accessor = accessors.get(nonNullField);
        if (accessor == null) {
            throw new IllegalArgumentException("Query field is not resolved: " + nonNullField.getName());
        }
        return (FieldAccessor<T, V>) accessor;
    }

    static final class FieldAccessor<T, V> {
        private final QueryField<V> field;
        private final Function<? super T, ? extends V> accessor;
        private final Comparator<? super V> comparator;

        FieldAccessor(
                QueryField<V> field,
                Function<? super T, ? extends V> accessor,
                Comparator<? super V> comparator
        ) {
            this.field = field;
            this.accessor = accessor;
            this.comparator = comparator;
        }

        private @Nullable V resolve(T candidate) {
            T nonNullCandidate = Objects.requireNonNull(candidate, "candidate");
            V value = accessor.apply(nonNullCandidate);
            if (value != null && !field.getValueType().isInstance(value)) {
                throw new IllegalStateException(
                        "Resolved query field value does not match field type: " + field.getName()
                );
            }
            return value;
        }

        private int compare(T left, T right) {
            V leftValue = resolve(left);
            V rightValue = resolve(right);
            if (leftValue == null) {
                return rightValue == null ? 0 : 1;
            }
            if (rightValue == null) {
                return -1;
            }
            return comparator.compare(leftValue, rightValue);
        }
    }

    static final class NaturalOrderComparator<V> implements Comparator<V> {
        @Override
        public int compare(V left, V right) {
            V nonNullLeft = Objects.requireNonNull(left, "left");
            V nonNullRight = Objects.requireNonNull(right, "right");
            if (!(nonNullLeft instanceof Comparable<?> comparable)) {
                throw new IllegalArgumentException("Query field values require an explicit ordering comparator");
            }
            try {
                return compareUnchecked(comparable, nonNullRight);
            } catch (ClassCastException exception) {
                throw new IllegalArgumentException(
                        "Query field values require an explicit ordering comparator",
                        exception
                );
            }
        }

        @SuppressWarnings("unchecked")
        private static int compareUnchecked(Comparable<?> left, Object right) {
            return ((Comparable<Object>) left).compareTo(right);
        }
    }
}
