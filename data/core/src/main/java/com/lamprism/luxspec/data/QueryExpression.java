package com.lamprism.luxspec.data;

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
     * @param <R> the visitor result type
     * @return the visitor result
     */
    <R> R accept(QueryExpressionVisitor<R> visitor);
}
