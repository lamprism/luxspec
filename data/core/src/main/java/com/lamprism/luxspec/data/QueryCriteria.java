package com.lamprism.luxspec.data;

import java.util.List;
import java.util.Objects;

/**
 * Groups one structured filter expression and explicit typed ordering.
 *
 * @author RollW
 */
public final class QueryCriteria {
    private static final QueryCriteria EMPTY = new QueryCriteria(QueryExpressions.all(), List.of());

    private final QueryExpression condition;
    private final List<OrderBy<?>> orders;

    /**
     * Creates immutable query criteria.
     *
     * @param condition the structured filter expression
     * @param orders explicit ordering terms
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
