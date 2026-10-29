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

import java.util.Objects;

/**
 * Tests whether an explicitly allowed related collection contains a matching candidate.
 *
 * <p>The nested expression is evaluated in the related type's query scope. Provider adapters may
 * translate this expression to an EXISTS subquery or an equivalent join.</p>
 *
 * @param <T> the related candidate type
 *
 * @author RollW
 */
public final class ExistsCondition<T> implements QueryExpression {
    private final QueryRelation<T> relation;
    private final QueryExpression condition;

    private ExistsCondition(QueryRelation<T> relation, QueryExpression condition) {
        if (relation.getCardinality() != QueryRelation.Cardinality.TO_MANY) {
            throw new IllegalArgumentException("EXISTS requires a to-many relation");
        }
        this.relation = Objects.requireNonNull(relation, "relation");
        this.condition = Objects.requireNonNull(condition, "condition");
    }

    /**
     * Creates an EXISTS expression over a related collection.
     *
     * @param relation  the allowed to-many relationship
     * @param condition the related candidate condition
     * @param <T>       the related candidate type
     * @return the immutable EXISTS expression
     */
    public static <T> ExistsCondition<T> of(
            QueryRelation<T> relation,
            QueryExpression condition
    ) {
        return new ExistsCondition<>(
                Objects.requireNonNull(relation, "relation"),
                Objects.requireNonNull(condition, "condition")
        );
    }

    /**
     * Returns the relationship searched by this expression.
     *
     * @return the related collection identity
     */
    public QueryRelation<T> getRelation() {
        return relation;
    }

    /**
     * Returns the related candidate condition.
     *
     * @return the nested condition
     */
    public QueryExpression getCondition() {
        return condition;
    }

    @Override
    public <R> R accept(QueryExpressionVisitor<R> visitor) {
        return Objects.requireNonNull(visitor, "visitor").visitExists(this);
    }
}
