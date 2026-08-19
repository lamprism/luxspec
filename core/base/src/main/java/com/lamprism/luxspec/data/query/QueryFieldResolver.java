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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Resolves typed query fields against candidate objects.
 *
 * <p>The resolver also owns the comparison rule required to order resolved field values. Query
 * operator and complexity policy remains owned by {@link QuerySchema}.</p>
 *
 * <p>This is an advanced execution SPI. Standard in-memory callers should use the high-level
 * executor or resource browser builder, which assembles the default resolver internally.</p>
 *
 * @param <T> the queried candidate type
 * @author RollW
 */
public interface QueryFieldResolver<T> {
    /**
     * Creates a resolver builder using direct candidate accessors.
     *
     * @param <T> the queried candidate type
     * @return the resolver builder
     */
    static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Resolves one typed field value from a candidate.
     *
     * @param candidate the candidate to inspect
     * @param field     the query field to resolve
     * @param <V>       the field value type
     * @return the resolved field value
     */
    <V> @Nullable V resolve(T candidate, QueryField<V> field);

    /**
     * Compares one field value from two candidates.
     *
     * @param left  the first candidate
     * @param right the second candidate
     * @param field the field used for comparison
     * @param <V>   the field value type
     * @return a negative value, zero, or a positive value when left sorts before, with, or after right
     */
    <V> int compare(T left, T right, QueryField<V> field);

    /**
     * Builds a resolver from candidate accessors.
     *
     * @param <T> the queried candidate type
     * @author RollW
     */
    final class Builder<T> {
        private final Map<QueryField<?>, DefaultQueryFieldResolver.FieldAccessor<T, ?>> accessors
                = new LinkedHashMap<>();

        private Builder() {
        }

        /**
         * Adds a query field using natural ordering for ordered queries.
         *
         * @param field    the resolved query field
         * @param accessor the value accessor for one candidate
         * @param <V>      the field value type
         * @return this builder
         */
        public <V> Builder<T> field(
                QueryField<V> field,
                Function<? super T, ? extends V> accessor
        ) {
            return field(field, accessor, new DefaultQueryFieldResolver.NaturalOrderComparator<>());
        }

        /**
         * Adds a query field using an explicit ordering rule.
         *
         * @param field      the resolved query field
         * @param accessor   the value accessor for one candidate
         * @param comparator the field ordering rule
         * @param <V>        the field value type
         * @return this builder
         */
        public <V> Builder<T> field(
                QueryField<V> field,
                Function<? super T, ? extends V> accessor,
                Comparator<? super V> comparator
        ) {
            QueryField<V> nonNullField = Objects.requireNonNull(field, "field");
            Function<? super T, ? extends V> nonNullAccessor = Objects.requireNonNull(accessor, "accessor");
            Comparator<? super V> nonNullComparator = Objects.requireNonNull(comparator, "comparator");
            if (accessors.containsKey(nonNullField)) {
                throw new IllegalArgumentException("Query field is already resolved: " + nonNullField.getName());
            }
            accessors.put(
                    nonNullField,
                    new DefaultQueryFieldResolver.FieldAccessor<>(
                            nonNullField,
                            nonNullAccessor,
                            nonNullComparator
                    )
            );
            return this;
        }

        /**
         * Creates the resolver.
         *
         * @return the immutable field resolver
         */
        public QueryFieldResolver<T> build() {
            return new DefaultQueryFieldResolver<>(Map.copyOf(accessors));
        }
    }
}
