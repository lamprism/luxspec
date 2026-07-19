package com.lamprism.luxspec.data;

final class MatchAllQueryExpression implements QueryExpression {
    @Override
    public <R> R accept(QueryExpressionVisitor<R> visitor) {
        return visitor.visitMatchAll();
    }
}
