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
 * A node in a provider-independent structured query filter.
 *
 * @author RollW
 */
public sealed interface QueryExpression permits QueryCondition, ComparisonCondition, LikeCondition,
        LogicalCondition, NegatedQueryExpression, MatchAllQueryExpression {
    /**
     * Dispatches this expression to a typed visitor.
     *
     * @param visitor the expression visitor
     * @param <R>     the visitor result type
     * @return the visitor result
     */
    <R> R accept(QueryExpressionVisitor<R> visitor);
}
