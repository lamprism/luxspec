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

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A node in a provider-independent structured query filter.
 *
 * <p>Application-owned executors may interpret additional nodes. The standard schema and
 * executors reject nodes outside the built-in model.</p>
 *
 * @author RollW
 */
public interface QueryExpression {
    /**
     * Returns an expression that accepts every candidate.
     *
     * @return the shared unconditional expression
     */
    static QueryExpression all() {
        return MatchAllQueryExpression.INSTANCE;
    }

    /**
     * Negates one expression.
     *
     * @param expression the expression to negate
     * @return the immutable negated expression
     */
    static NegatedQueryExpression not(QueryExpression expression) {
        return NegatedQueryExpression.of(Objects.requireNonNull(expression, "expression"));
    }

    /**
     * Combines expressions with logical conjunction.
     *
     * @param expressions the expressions to combine
     * @return the immutable conjunction
     */
    static LogicalCondition and(List<? extends QueryExpression> expressions) {
        return LogicalCondition.and(expressions);
    }

    /**
     * Combines expressions with logical disjunction.
     *
     * @param expressions the expressions to combine
     * @return the immutable disjunction
     */
    static LogicalCondition or(List<? extends QueryExpression> expressions) {
        return LogicalCondition.or(expressions);
    }

    /**
     * Reports whether a built-in expression contains an aggregate value.
     *
     * <p>Application-owned nodes are not traversed and require their own validation.</p>
     *
     * @param expression the expression to inspect
     * @return true when a built-in node contains an aggregate
     */
    static boolean containsAggregate(QueryExpression expression) {
        Deque<QueryExpression> pending = new ArrayDeque<>();
        Set<QueryExpression> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        pending.push(Objects.requireNonNull(expression, "expression"));
        while (!pending.isEmpty()) {
            QueryExpression current = pending.pop();
            if (!visited.add(current)) {
                continue;
            }
            if (current instanceof QueryValueCondition<?, ?> condition
                    && QueryValue.containsAggregate(condition.getValue())) {
                return true;
            }
            if (current instanceof LogicalCondition condition) {
                pending.addAll(condition.getExpressions());
            } else if (current instanceof NegatedQueryExpression condition) {
                pending.push(condition.getExpression());
            } else if (current instanceof ExistsCondition<?> condition) {
                pending.push(condition.getCondition());
            }
        }
        return false;
    }

    /**
     * Reports whether a node belongs to the standard expression model.
     *
     * @param expression the expression node to inspect
     * @return true for a built-in node
     */
    static boolean isBuiltIn(QueryExpression expression) {
        QueryExpression node = Objects.requireNonNull(expression, "expression");
        return node == MatchAllQueryExpression.INSTANCE || node instanceof QueryCondition<?, ?>
                || node instanceof QueryValueCondition<?, ?> || node instanceof ComparisonCondition<?>
                || node instanceof LikeCondition || node instanceof LogicalCondition
                || node instanceof NegatedQueryExpression || node instanceof ExistsCondition<?>;
    }

    /**
     * Dispatches this expression to a typed visitor.
     *
     * <p>Application-owned nodes may use their own dispatch protocol. The default rejects the
     * built-in visitor because it does not define application-specific operations.</p>
     *
     * @param visitor the expression visitor
     * @param <R>     the visitor result type
     * @return the visitor result
     */
    default <R> R accept(QueryExpressionVisitor<R> visitor) {
        Objects.requireNonNull(visitor, "visitor");
        throw new UnsupportedOperationException("Application query expression does not support the standard visitor");
    }

}
