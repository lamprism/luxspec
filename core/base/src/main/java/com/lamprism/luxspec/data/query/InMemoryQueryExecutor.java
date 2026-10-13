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

import com.lamprism.luxspec.data.pagination.QueryResult;
import com.lamprism.luxspec.data.pagination.QueryWindow;
import com.lamprism.luxspec.data.query.inmemory.InMemoryQueryEngine;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Executes validated structured queries over a supplied in-memory candidate collection.
 *
 * <p>A supplied candidate filter is evaluated after structured criteria and before ordering,
 * counting, and windowing. The candidate supplier is called once per operation.</p>
 *
 * <p>The executor does not validate which fields, operators, ordering, or complexity a role
 * permits. The owning reader must validate criteria with its {@link QuerySchema} and
 * {@link QueryComplexityBudget} before calling this executor. Every matching candidate is
 * materialized in memory before ordering and windowing, including an unbounded result, so this
 * implementation is intended for bounded application-owned collections rather than persistent or
 * arbitrarily large data sets.</p>
 *
 * <p>A {@link com.lamprism.luxspec.data.pagination.TimeSeriesWindow} uses
 * {@link TimeSeriesPoint#TIMESTAMP} as a required non-null timestamp field. Register that field
 * like any other query field when time-series windows are used.</p>
 *
 * @param <T> the queried candidate type
 * @author RollW
 */
public class InMemoryQueryExecutor<T>
        implements QueryExecutor<T> {
    private final InMemoryQueryEngine<T> engine;

    /**
     * Starts configuring an in-memory query executor without exposing resolver assembly details.
     *
     * @param candidateSupplier the in-memory candidate source evaluated once per operation
     * @param <T>               the queried candidate type
     * @return a new executor builder
     */
    public static <T> Builder<T> builder(
            Supplier<? extends Collection<? extends T>> candidateSupplier
    ) {
        return new Builder<>(candidateSupplier);
    }

    /**
     * Creates an executor using one candidate supplier and an explicit field resolver.
     *
     * <p>This constructor is an advanced extension point for custom field resolution. Use
     * {@link #builder(Supplier)} for the standard construction path.</p>
     *
     * @param candidateSupplier the in-memory candidate source evaluated once per operation
     * @param fieldResolver     the query field resolver
     */
    public InMemoryQueryExecutor(
            Supplier<? extends Collection<? extends T>> candidateSupplier,
            QueryFieldResolver<T> fieldResolver
    ) {
        this(candidateSupplier, fieldResolver, (candidate, relation) -> null, Map.of(), Map.of());
    }

    /**
     * Creates an executor with explicit relationship and related-field resolution.
     *
     * @param candidateSupplier     the in-memory candidate source
     * @param fieldResolver         the root field resolver
     * @param relationResolver      the allowed relationship resolver
     * @param relatedFieldResolvers field resolvers keyed by relationship
     */
    public InMemoryQueryExecutor(
            Supplier<? extends Collection<? extends T>> candidateSupplier,
            QueryFieldResolver<T> fieldResolver,
            QueryRelationResolver<T> relationResolver,
            Map<? extends QueryRelation<?>, ? extends QueryFieldResolver<?>> relatedFieldResolvers
    ) {
        this(candidateSupplier, fieldResolver, relationResolver, relatedFieldResolvers, Map.of());
    }

    /**
     * Creates an executor with explicit relationship, nested relationship, and related-field
     * resolution.
     *
     * @param candidateSupplier        the in-memory candidate source
     * @param fieldResolver            the root field resolver
     * @param relationResolver         the root relationship resolver
     * @param relatedFieldResolvers    field resolvers keyed by relationship
     * @param relatedRelationResolvers relationship resolvers keyed by nested relationship
     */
    public InMemoryQueryExecutor(
            Supplier<? extends Collection<? extends T>> candidateSupplier,
            QueryFieldResolver<T> fieldResolver,
            QueryRelationResolver<T> relationResolver,
            Map<? extends QueryRelation<?>, ? extends QueryFieldResolver<?>> relatedFieldResolvers,
            Map<? extends QueryRelation<?>, ? extends QueryRelationResolver<?>> relatedRelationResolvers
    ) {
        this.engine = new InMemoryQueryEngine<>(
                candidateSupplier,
                fieldResolver,
                relationResolver,
                copy(relatedFieldResolvers),
                copy(relatedRelationResolvers)
        );
    }

    /**
     * Executes criteria over every candidate supplied for this operation.
     *
     * @param criteria the validated filters and ordering
     * @param window   the requested result window
     * @return the complete, page, or slice result
     */
    @Override
    public QueryResult<T> query(QueryCriteria criteria, QueryWindow window) {
        return engine.query(criteria, window);
    }

    /**
     * Executes criteria over candidates accepted by an in-memory post-condition filter.
     *
     * @param candidateFilter the restriction applied before ordering and windowing
     * @param criteria        the validated filters and ordering
     * @param window          the requested result window
     * @return the complete, page, or slice result
     */
    public QueryResult<T> query(
            Predicate<? super T> candidateFilter,
            QueryCriteria criteria,
            QueryWindow window
    ) {
        return engine.query(candidateFilter, criteria, window);
    }

    /**
     * Executes a projection, aggregate, or time-series plan over the supplied in-memory
     * candidates.
     *
     * @param plan   the validated query plan
     * @param window the requested result window
     * @param <R>    the plan result type
     * @return the plan result
     */
    @Override
    public <R> QueryResult<R> query(QueryPlan<R> plan, QueryWindow window) {
        return engine.query(plan, window);
    }

    private static <K, V> Map<K, V> copy(
            Map<? extends K, ? extends V> values
    ) {
        return Map.copyOf(Objects.requireNonNull(values, "values"));
    }

    /**
     * Configures an in-memory query executor.
     *
     * @param <T> the queried candidate type
     */
    public static class Builder<T> {
        private final Supplier<? extends Collection<? extends T>> candidateSupplier;
        private final QueryFieldResolver.Builder<T> fieldResolverBuilder = QueryFieldResolver.builder();
        private final Map<QueryRelation<?>, RelationBinding<T, ?>> relationBindings = new LinkedHashMap<>();
        private final Map<QueryRelation<?>, QueryFieldResolver<?>> relatedFieldResolvers = new LinkedHashMap<>();
        private final Map<QueryRelation<?>, QueryRelationResolver<?>> relatedRelationResolvers = new LinkedHashMap<>();

        private Builder(
                Supplier<? extends Collection<? extends T>> candidateSupplier
        ) {
            this.candidateSupplier = Objects.requireNonNull(candidateSupplier, "candidateSupplier");
        }

        /**
         * Registers a queryable field using its natural value ordering.
         *
         * @param field    the query field
         * @param accessor the candidate value accessor
         * @param <V>      the field value type
         * @return this builder
         */
        public <V> Builder<T> field(
                QueryField<V> field,
                Function<? super T, ? extends @Nullable V> accessor
        ) {
            fieldResolverBuilder.field(field, accessor);
            return this;
        }

        /**
         * Registers an allowed to-many relationship and its related-field resolver.
         *
         * @param relation      the relationship identity
         * @param accessor      the related candidate accessor
         * @param fieldResolver the related candidate field resolver
         * @param <R>           the related candidate type
         * @return this builder
         */
        public <R> Builder<T> relationToMany(
                QueryRelation<R> relation,
                Function<? super T, ? extends Iterable<? extends R>> accessor,
                QueryFieldResolver<R> fieldResolver
        ) {
            QueryRelation<R> nonNullRelation = requireToMany(relation);
            RelationBinding<T, R> binding = new RelationBinding<>(
                    Objects.requireNonNull(accessor, "accessor"),
                    Objects.requireNonNull(fieldResolver, "fieldResolver")
            );
            if (relationBindings.putIfAbsent(nonNullRelation, binding) != null
                    || relatedRelationResolvers.containsKey(nonNullRelation)) {
                relationBindings.remove(nonNullRelation, binding);
                throw duplicateRelation(nonNullRelation);
            }
            return this;
        }

        /**
         * Registers a relationship resolver for a relation inside another relation scope.
         *
         * <p>The resolver uses the source candidate type of that nested scope. Registering nested
         * relations through the same builder keeps field and relationship resolution in one
         * configuration, while the public resolver contract remains the existing
         * {@link QueryRelationResolver} interface.</p>
         *
         * @param relation      the relationship identity
         * @param resolver      the source-candidate relationship resolver
         * @param fieldResolver the related candidate field resolver
         * @param <S>           the source candidate type
         * @param <R>           the related candidate type
         * @return this builder
         */
        public <S, R> Builder<T> relationToMany(
                QueryRelation<R> relation,
                QueryRelationResolver<S> resolver,
                QueryFieldResolver<R> fieldResolver
        ) {
            QueryRelation<R> nonNullRelation = requireToMany(relation);
            QueryRelationResolver<S> nonNullResolver = Objects.requireNonNull(resolver, "resolver");
            QueryFieldResolver<R> nonNullFieldResolver = Objects.requireNonNull(fieldResolver, "fieldResolver");
            if (relationBindings.containsKey(nonNullRelation)
                    || relatedRelationResolvers.containsKey(nonNullRelation)) {
                throw duplicateRelation(nonNullRelation);
            }
            relatedRelationResolvers.put(nonNullRelation, nonNullResolver);
            relatedFieldResolvers.put(nonNullRelation, nonNullFieldResolver);
            return this;
        }

        /**
         * Registers a queryable field with an explicit value comparator.
         *
         * @param field      the query field
         * @param accessor   the candidate value accessor
         * @param comparator the field value comparator
         * @param <V>        the field value type
         * @return this builder
         */
        public <V> Builder<T> field(
                QueryField<V> field,
                Function<? super T, ? extends @Nullable V> accessor,
                Comparator<? super V> comparator
        ) {
            fieldResolverBuilder.field(field, accessor, comparator);
            return this;
        }

        /**
         * Creates the configured executor.
         *
         * @return a new in-memory query executor
         */
        public InMemoryQueryExecutor<T> build() {
            Map<QueryRelation<?>, QueryFieldResolver<?>> resolvers = new LinkedHashMap<>(relatedFieldResolvers);
            for (Map.Entry<QueryRelation<?>, RelationBinding<T, ?>> entry : relationBindings.entrySet()) {
                resolvers.put(entry.getKey(), entry.getValue().fieldResolver());
            }
            QueryRelationResolver<T> relationResolver = (candidate, relation) -> {
                RelationBinding<T, ?> binding = relationBindings.get(relation);
                if (binding == null) {
                    throw new IllegalArgumentException("Query relation is not resolved: " + relation.getName());
                }
                return binding.resolve(candidate);
            };
            return new InMemoryQueryExecutor<>(
                    candidateSupplier,
                    fieldResolverBuilder.build(),
                    relationResolver,
                    resolvers,
                    relatedRelationResolvers
            );
        }

        private static <R> QueryRelation<R> requireToMany(QueryRelation<R> relation) {
            QueryRelation<R> nonNullRelation = Objects.requireNonNull(relation, "relation");
            if (nonNullRelation.getCardinality() != QueryRelation.Cardinality.TO_MANY) {
                throw new IllegalArgumentException("relationToMany requires a to-many relation");
            }
            return nonNullRelation;
        }

        private static IllegalArgumentException duplicateRelation(QueryRelation<?> relation) {
            return new IllegalArgumentException("Query relation is already resolved: " + relation.getName());
        }
    }

    private record RelationBinding<T, R>(
            Function<? super T, ? extends Iterable<? extends R>> accessor,
            QueryFieldResolver<R> fieldResolver
    ) {
        private Iterable<? extends R> resolve(T candidate) {
            return accessor.apply(Objects.requireNonNull(candidate, "candidate"));
        }
    }
}
