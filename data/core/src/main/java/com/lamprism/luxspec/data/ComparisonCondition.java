package com.lamprism.luxspec.data;

import java.util.Objects;

/**
 * A typed ordered comparison for a comparable query field.
 *
 * @param <T> the comparable field value type
 * @author RollW
 */
public final class ComparisonCondition<T extends Comparable<? super T>> implements QueryExpression {
    private final QueryField<T> field;
    private final QueryOperator operator;
    private final T value;

    private ComparisonCondition(QueryField<T> field, QueryOperator operator, T value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

    /**
     * Creates a greater-than condition.
     *
     * @param field the typed comparable field
     * @param value the comparison value
     * @param <T> the comparable field value type
     * @return the immutable condition
     */
    public static <T extends Comparable<? super T>> ComparisonCondition<T> greaterThan(QueryField<T> field, T value) {
        return create(field, QueryOperator.GREATER_THAN, value);
    }

    /**
     * Creates a greater-than-or-equal condition.
     *
     * @param field the typed comparable field
     * @param value the comparison value
     * @param <T> the comparable field value type
     * @return the immutable condition
     */
    public static <T extends Comparable<? super T>> ComparisonCondition<T> greaterThanOrEqualTo(QueryField<T> field, T value) {
        return create(field, QueryOperator.GREATER_THAN_OR_EQUAL, value);
    }

    /**
     * Creates a less-than condition.
     *
     * @param field the typed comparable field
     * @param value the comparison value
     * @param <T> the comparable field value type
     * @return the immutable condition
     */
    public static <T extends Comparable<? super T>> ComparisonCondition<T> lessThan(QueryField<T> field, T value) {
        return create(field, QueryOperator.LESS_THAN, value);
    }

    /**
     * Creates a less-than-or-equal condition.
     *
     * @param field the typed comparable field
     * @param value the comparison value
     * @param <T> the comparable field value type
     * @return the immutable condition
     */
    public static <T extends Comparable<? super T>> ComparisonCondition<T> lessThanOrEqualTo(QueryField<T> field, T value) {
        return create(field, QueryOperator.LESS_THAN_OR_EQUAL, value);
    }

    /**
     * Returns the compared field.
     *
     * @return the typed comparable field
     */
    public QueryField<T> getField() {
        return field;
    }

    /**
     * Returns the ordered comparison operator.
     *
     * @return the comparison operator
     */
    public QueryOperator getOperator() {
        return operator;
    }

    /**
     * Returns the comparison value.
     *
     * @return the typed comparison value
     */
    public T getValue() {
        return value;
    }

    @Override
    public <R> R accept(QueryExpressionVisitor<R> visitor) {
        return Objects.requireNonNull(visitor, "visitor").visitComparison(this);
    }

    private static <T extends Comparable<? super T>> ComparisonCondition<T> create(
            QueryField<T> field,
            QueryOperator operator,
            T value
    ) {
        QueryField<T> nonNullField = Objects.requireNonNull(field, "field");
        nonNullField.requireValue(value);
        return new ComparisonCondition<>(nonNullField, operator, value);
    }
}
