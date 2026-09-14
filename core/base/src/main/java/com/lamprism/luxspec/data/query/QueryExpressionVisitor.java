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

/**
 * Visits every supported structured query expression node.
 *
 * @param <R> the visitor result type
 * @author RollW
 */
public interface QueryExpressionVisitor<R> {
    /**
     * Visits an unconditional expression.
     *
     * @return the visitor result
     */
    R visitMatchAll();

    /**
     * Visits a typed equality or membership condition.
     *
     * @param condition the condition
     * @param <T>       the field value type
     * @return the visitor result
     */
    <T> R visitCondition(QueryCondition<T> condition);

    /**
     * Visits a typed ordered comparison condition.
     *
     * @param condition the comparison condition
     * @param <T>       the comparable field value type
     * @return the visitor result
     */
    <T extends Comparable<? super T>> R visitComparison(ComparisonCondition<T> condition);

    /**
     * Visits a string pattern condition.
     *
     * @param condition the LIKE condition
     * @return the visitor result
     */
    R visitLike(LikeCondition condition);

    /**
     * Visits a logical group.
     *
     * @param condition the logical group
     * @return the visitor result
     */
    R visitLogical(LogicalCondition condition);

    /**
     * Visits a negated expression.
     *
     * @param condition the negated expression
     * @return the visitor result
     */
    R visitNegated(NegatedQueryExpression condition);
}
