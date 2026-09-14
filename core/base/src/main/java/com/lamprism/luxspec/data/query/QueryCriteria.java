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
 * Groups one structured filter expression and explicit typed ordering.
 *
 * @author RollW
 */
public final class QueryCriteria {
    private static final QueryCriteria EMPTY = new QueryCriteria(QueryExpression.all(), List.of());

    private final QueryExpression condition;
    private final List<OrderBy<?>> orders;

    /**
     * Creates immutable query criteria.
     *
     * @param condition the structured filter expression
     * @param orders    explicit ordering terms
     */
    public QueryCriteria(QueryExpression condition, List<? extends OrderBy<?>> orders) {
        this.condition = Objects.requireNonNull(condition, "condition");
        this.orders = List.copyOf(Objects.requireNonNull(orders, "orders"));
    }

    /**
     * Returns unfiltered and unordered criteria.
     *
     * @return the shared empty criteria
     */
    public static QueryCriteria empty() {
        return EMPTY;
    }

    /**
     * Returns the complete structured filter expression.
     *
     * @return the filter expression
     */
    public QueryExpression getCondition() {
        return condition;
    }

    /**
     * Returns immutable typed ordering terms.
     *
     * @return query ordering
     */
    public List<OrderBy<?>> getOrders() {
        return orders;
    }
}
