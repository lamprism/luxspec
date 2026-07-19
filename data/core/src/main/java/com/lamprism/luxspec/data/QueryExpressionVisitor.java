package com.lamprism.luxspec.data;

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
     * @param <T> the field value type
     * @return the visitor result
     */
    <T> R visitCondition(QueryCondition<T> condition);

    /**
     * Visits a typed ordered comparison condition.
     *
     * @param condition the comparison condition
     * @param <T> the comparable field value type
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
