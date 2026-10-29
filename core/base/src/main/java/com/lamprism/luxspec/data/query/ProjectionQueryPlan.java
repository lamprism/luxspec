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
import java.util.List;
import java.util.Objects;

/**
 * Query plan for direct projections, aggregates, grouping, and HAVING filters.
 *
 * <p>This type owns all projection-specific state. A time-series plan is represented by
 * {@link TimeSeriesQueryPlan} instead of by nullable fields on this plan.</p>
 *
 * @param <R> the projected result type
 *
 * @author RollW
 */
public final class ProjectionQueryPlan<R>
        implements QueryPlan<R> {
    private final QueryCriteria criteria;
    private final QueryProjection<R> projection;
    private final boolean distinct;
    private final List<QueryValue<?>> groupBy;
    private final @Nullable QueryExpression having;

    private ProjectionQueryPlan(Builder<R> builder) {
        criteria = builder.criteria;
        projection = builder.projection;
        distinct = builder.distinct;
        groupBy = List.copyOf(builder.groupBy);
        having = builder.having;
        if (having != null && groupBy.isEmpty() && !containsAggregate(projection.getValues())) {
            throw new IllegalArgumentException("HAVING requires grouping or an aggregate projection");
        }
    }

    /**
     * Starts a projection plan.
     *
     * @param projection the typed projection
     * @param <R>        the projected result type
     * @return the plan builder
     */
    public static <R> Builder<R> builder(QueryProjection<R> projection) {
        return new Builder<>(projection);
    }

    /**
     * Starts a plan that projects one typed value.
     *
     * @param value the selected value
     * @param <R>   the projected value type
     * @return the plan builder
     */
    public static <R> Builder<R> builder(QueryValue<R> value) {
        return builder(QueryProjection.single(value));
    }

    /**
     * Returns the source criteria.
     *
     * @return the source criteria
     */
    @Override
    public QueryCriteria getCriteria() {
        return criteria;
    }

    /**
     * Returns the typed projection.
     *
     * @return the projection
     */
    public QueryProjection<R> getProjection() {
        return projection;
    }

    /**
     * Reports whether duplicate projected rows are removed.
     *
     * @return true for distinct results
     */
    public boolean isDistinct() {
        return distinct;
    }

    /**
     * Returns the grouping expressions.
     *
     * @return immutable grouping expressions
     */
    public List<QueryValue<?>> getGroupBy() {
        return groupBy;
    }

    /**
     * Returns the optional HAVING condition.
     *
     * @return the HAVING condition, or null
     */
    public @Nullable QueryExpression getHaving() {
        return having;
    }

    /**
     * Reports whether the plan contains an aggregate in its projection, grouping, or HAVING
     * expression.
     *
     * @return true when the plan contains an aggregate
     */
    public boolean containsAggregate() {
        if (containsAggregate(projection.getValues())) {
            return true;
        }
        if (containsAggregate(groupBy)) {
            return true;
        }
        return having != null && QueryExpression.containsAggregate(having);
    }

    private static boolean containsAggregate(List<? extends QueryValue<?>> values) {
        for (QueryValue<?> value : values) {
            if (QueryValue.containsAggregate(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds an immutable projection query plan.
     *
     * @param <R> the projected result type
     */
    public static final class Builder<R> {
        private final QueryProjection<R> projection;
        private QueryCriteria criteria = QueryCriteria.empty();
        private boolean distinct;
        private final List<QueryValue<?>> groupBy = new ArrayList<>();
        private @Nullable QueryExpression having;

        private Builder(QueryProjection<R> projection) {
            this.projection = Objects.requireNonNull(projection, "projection");
        }

        /**
         * Sets source filtering and ordering.
         *
         * @param criteria the source criteria
         * @return this builder
         */
        public Builder<R> criteria(QueryCriteria criteria) {
            this.criteria = Objects.requireNonNull(criteria, "criteria");
            return this;
        }

        /**
         * Enables duplicate row elimination after projection.
         *
         * @return this builder
         */
        public Builder<R> distinct() {
            distinct = true;
            return this;
        }

        /**
         * Adds one grouping expression.
         *
         * @param value the grouping expression
         * @return this builder
         */
        public Builder<R> groupBy(QueryValue<?> value) {
            groupBy.add(Objects.requireNonNull(value, "value"));
            return this;
        }

        /**
         * Sets the aggregate or group filter.
         *
         * @param having the HAVING condition
         * @return this builder
         */
        public Builder<R> having(QueryExpression having) {
            this.having = Objects.requireNonNull(having, "having");
            return this;
        }

        /**
         * Creates the immutable projection plan.
         *
         * @return the projection plan
         */
        public ProjectionQueryPlan<R> build() {
            return new ProjectionQueryPlan<>(this);
        }
    }
}
