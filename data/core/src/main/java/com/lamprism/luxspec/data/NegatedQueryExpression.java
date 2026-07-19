package com.lamprism.luxspec.data;

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
