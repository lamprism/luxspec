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

import java.util.List;
import java.util.Objects;

/**
 * A non-empty logical group of structured query expressions.
 *
 * @author RollW
 */
public final class LogicalCondition implements QueryExpression {
    private final LogicalOperator operator;
    private final List<QueryExpression> expressions;

    private LogicalCondition(LogicalOperator operator, List<QueryExpression> expressions) {
        this.operator = operator;
        this.expressions = expressions;
    }

    /**
     * Creates a conjunction of two or more expressions.
     *
     * @param expressions the grouped expressions
     * @return the immutable conjunction
     */
    public static LogicalCondition and(List<? extends QueryExpression> expressions) {
        return create(LogicalOperator.AND, expressions);
    }

    /**
     * Creates a disjunction of two or more expressions.
     *
     * @param expressions the grouped expressions
     * @return the immutable disjunction
     */
    public static LogicalCondition or(List<? extends QueryExpression> expressions) {
        return create(LogicalOperator.OR, expressions);
    }

    /**
     * Returns the logical connective.
     *
     * @return the connective
     */
    public LogicalOperator getOperator() {
        return operator;
    }

    /**
     * Returns immutable child expressions.
     *
     * @return the grouped expressions
     */
    public List<QueryExpression> getExpressions() {
        return expressions;
    }

    @Override
    public <R> R accept(QueryExpressionVisitor<R> visitor) {
        return Objects.requireNonNull(visitor, "visitor").visitLogical(this);
    }

    private static LogicalCondition create(LogicalOperator operator, List<? extends QueryExpression> expressions) {
        Objects.requireNonNull(operator, "operator");
        List<QueryExpression> copiedExpressions = List.copyOf(Objects.requireNonNull(expressions, "expressions"));
        if (copiedExpressions.size() < 2) {
            throw new IllegalArgumentException("Logical groups must contain at least two expressions");
        }
        return new LogicalCondition(operator, copiedExpressions);
    }
}
