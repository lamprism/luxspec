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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A typed condition over a transformed {@link QueryValue}.
 *
 * <p>Existing field-specific condition types remain the concise API for direct fields. This
 * condition is the single general predicate form for lower/upper/coalesce/cast values and avoids
 * one new condition type per supported function.</p>
 *
 * @param <T> the value expression type
 * @author RollW
 */
public final class QueryValueCondition<T> implements QueryExpression {
    private final QueryValue<T> value;
    private final QueryOperator operator;
    private final List<T> values;

    private QueryValueCondition(QueryValue<T> value, QueryOperator operator, List<T> values) {
        this.value = value;
        this.operator = operator;
        this.values = values;
    }

    /**
     * Creates an equality condition.
     *
     * @param value      the value expression
     * @param comparison the required comparison value
     * @param <T>        the value type
     * @return the immutable condition
     */
    public static <T> QueryValueCondition<T> equal(QueryValue<T> value, T comparison) {
        return singleValue(value, QueryOperator.EQUAL, comparison);
    }

    /**
     * Creates a non-equality condition.
     *
     * @param value      the value expression
     * @param comparison the required comparison value
     * @param <T>        the value type
     * @return the immutable condition
     */
    public static <T> QueryValueCondition<T> notEqual(QueryValue<T> value, T comparison) {
        return singleValue(value, QueryOperator.NOT_EQUAL, comparison);
    }

    /**
     * Creates a greater-than condition.
     *
     * @param value      the comparable value expression
     * @param comparison the comparison value
     * @param <T>        the comparable type
     * @return the immutable condition
     */
    public static <T extends Comparable<? super T>> QueryValueCondition<T> greaterThan(
            QueryValue<T> value,
            T comparison
    ) {
        return singleValue(value, QueryOperator.GREATER_THAN, comparison);
    }

    /**
     * Creates a greater-than-or-equal condition.
     *
     * @param value      the comparable value expression
     * @param comparison the comparison value
     * @param <T>        the comparable type
     * @return the immutable condition
     */
    public static <T extends Comparable<? super T>> QueryValueCondition<T> greaterThanOrEqualTo(
            QueryValue<T> value,
            T comparison
    ) {
        return singleValue(value, QueryOperator.GREATER_THAN_OR_EQUAL, comparison);
    }

    /**
     * Creates a less-than condition.
     *
     * @param value      the comparable value expression
     * @param comparison the comparison value
     * @param <T>        the comparable type
     * @return the immutable condition
     */
    public static <T extends Comparable<? super T>> QueryValueCondition<T> lessThan(
            QueryValue<T> value,
            T comparison
    ) {
        return singleValue(value, QueryOperator.LESS_THAN, comparison);
    }

    /**
     * Creates a less-than-or-equal condition.
     *
     * @param value      the comparable value expression
     * @param comparison the comparison value
     * @param <T>        the comparable type
     * @return the immutable condition
     */
    public static <T extends Comparable<? super T>> QueryValueCondition<T> lessThanOrEqualTo(
            QueryValue<T> value,
            T comparison
    ) {
        return singleValue(value, QueryOperator.LESS_THAN_OR_EQUAL, comparison);
    }

    /**
     * Creates a LIKE condition over a string value expression.
     *
     * @param value   the string value expression
     * @param pattern the validated LIKE pattern
     * @return the immutable condition
     */
    public static QueryValueCondition<String> like(QueryValue<String> value, String pattern) {
        QueryValue<String> nonNullValue = Objects.requireNonNull(value, "value");
        String nonNullPattern = Objects.requireNonNull(pattern, "pattern");
        LikeCondition.requireValidPattern(nonNullPattern);
        if (nonNullValue.getValueType() != String.class) {
            throw new IllegalArgumentException("LIKE requires a String value");
        }
        return new QueryValueCondition<>(nonNullValue, QueryOperator.LIKE, List.of(nonNullPattern));
    }

    /**
     * Creates an equality membership condition.
     *
     * @param value  the value expression
     * @param values the non-empty values
     * @param <T>    the value type
     * @return the immutable condition
     */
    public static <T> QueryValueCondition<T> in(QueryValue<T> value, Collection<? extends T> values) {
        return multipleValues(value, QueryOperator.IN, values);
    }

    /**
     * Creates a negative membership condition.
     *
     * @param value  the value expression
     * @param values the non-empty values
     * @param <T>    the value type
     * @return the immutable condition
     */
    public static <T> QueryValueCondition<T> notIn(QueryValue<T> value, Collection<? extends T> values) {
        return multipleValues(value, QueryOperator.NOT_IN, values);
    }

    /**
     * Creates a null-value condition.
     *
     * @param value the value expression
     * @param <T>   the value type
     * @return the immutable condition
     */
    public static <T> QueryValueCondition<T> isNull(QueryValue<T> value) {
        return noValue(value, QueryOperator.IS_NULL);
    }

    /**
     * Creates a non-null-value condition.
     *
     * @param value the value expression
     * @param <T>   the value type
     * @return the immutable condition
     */
    public static <T> QueryValueCondition<T> isNotNull(QueryValue<T> value) {
        return noValue(value, QueryOperator.IS_NOT_NULL);
    }

    /**
     * Returns the value expression.
     *
     * @return the typed value expression
     */
    public QueryValue<T> getValue() {
        return value;
    }

    /**
     * Returns the condition operator.
     *
     * @return the operator
     */
    public QueryOperator getOperator() {
        return operator;
    }

    /**
     * Returns immutable comparison values. Null predicates have no values.
     *
     * @return the comparison values
     */
    public List<T> getValues() {
        return values;
    }

    @Override
    public <R> R accept(QueryExpressionVisitor<R> visitor) {
        return Objects.requireNonNull(visitor, "visitor").visitValueCondition(this);
    }

    private static <T> QueryValueCondition<T> singleValue(
            QueryValue<T> value,
            QueryOperator operator,
            T comparison
    ) {
        QueryValue<T> nonNullValue = Objects.requireNonNull(value, "value");
        requireSingleValueOperator(operator);
        requireValue(nonNullValue, comparison);
        return new QueryValueCondition<>(nonNullValue, operator, List.of(comparison));
    }

    private static <T> QueryValueCondition<T> noValue(QueryValue<T> value, QueryOperator operator) {
        QueryValue<T> nonNullValue = Objects.requireNonNull(value, "value");
        if (operator != QueryOperator.IS_NULL && operator != QueryOperator.IS_NOT_NULL) {
            throw new IllegalArgumentException("Operator does not accept an empty value list");
        }
        return new QueryValueCondition<>(nonNullValue, operator, List.of());
    }

    private static <T> QueryValueCondition<T> multipleValues(
            QueryValue<T> value,
            QueryOperator operator,
            Collection<? extends T> values
    ) {
        QueryValue<T> nonNullValue = Objects.requireNonNull(value, "value");
        if (operator != QueryOperator.IN && operator != QueryOperator.NOT_IN) {
            throw new IllegalArgumentException("Operator does not accept membership values");
        }
        Objects.requireNonNull(values, "values");
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Membership values must not be empty");
        }
        List<T> copiedValues = new ArrayList<>(values.size());
        for (T candidate : values) {
            requireValue(nonNullValue, candidate);
            copiedValues.add(candidate);
        }
        return new QueryValueCondition<>(nonNullValue, operator, List.copyOf(copiedValues));
    }

    private static void requireSingleValueOperator(QueryOperator operator) {
        if (operator != QueryOperator.EQUAL
                && operator != QueryOperator.NOT_EQUAL
                && operator != QueryOperator.GREATER_THAN
                && operator != QueryOperator.GREATER_THAN_OR_EQUAL
                && operator != QueryOperator.LESS_THAN
                && operator != QueryOperator.LESS_THAN_OR_EQUAL) {
            throw new IllegalArgumentException("Operator does not accept one comparison value");
        }
    }

    private static <T> void requireValue(QueryValue<T> value, T candidate) {
        T nonNullCandidate = Objects.requireNonNull(candidate, "value");
        if (!value.getValueType().isInstance(nonNullCandidate)) {
            throw new IllegalArgumentException("Query value does not match expression type");
        }
    }
}
