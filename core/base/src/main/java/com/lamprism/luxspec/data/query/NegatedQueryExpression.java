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
 * A logical negation of one structured query expression.
 *
 * @author RollW
 */
public final class NegatedQueryExpression implements QueryExpression {
    private final QueryExpression expression;

    private NegatedQueryExpression(QueryExpression expression) {
        this.expression = expression;
    }

    /**
     * Creates a logical negation.
     *
     * @param expression the expression to negate
     * @return the immutable negated expression
     */
    public static NegatedQueryExpression of(QueryExpression expression) {
        return new NegatedQueryExpression(Objects.requireNonNull(expression, "expression"));
    }

    /**
     * Returns the expression being negated.
     *
     * @return the operand expression
     */
    public QueryExpression getExpression() {
        return expression;
    }

    @Override
    public <R> R accept(QueryExpressionVisitor<R> visitor) {
        return Objects.requireNonNull(visitor, "visitor").visitNegated(this);
    }
}
