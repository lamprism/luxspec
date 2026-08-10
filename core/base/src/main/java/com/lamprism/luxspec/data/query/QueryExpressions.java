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
 * Creates compound structured query expressions.
 *
 * @author RollW
 */
public final class QueryExpressions {
    private static final QueryExpression MATCH_ALL = new MatchAllQueryExpression();

    private QueryExpressions() {
    }

    /**
     * Returns an expression that accepts every candidate.
     *
     * @return the shared unconditional expression
     */
    public static QueryExpression all() {
        return MATCH_ALL;
    }

    /**
     * Negates one expression.
     *
     * @param expression the expression to negate
     * @return the immutable negated expression
     */
    public static NegatedQueryExpression not(QueryExpression expression) {
        return NegatedQueryExpression.of(Objects.requireNonNull(expression, "expression"));
    }

    /**
     * Combines expressions with logical conjunction.
     *
     * @param expressions the expressions to combine
     * @return the immutable conjunction
     */
    public static LogicalCondition and(List<? extends QueryExpression> expressions) {
        return LogicalCondition.and(expressions);
    }

    /**
     * Combines expressions with logical disjunction.
     *
     * @param expressions the expressions to combine
     * @return the immutable disjunction
     */
    public static LogicalCondition or(List<? extends QueryExpression> expressions) {
        return LogicalCondition.or(expressions);
    }
}
