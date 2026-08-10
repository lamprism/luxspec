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
 * A typed equality or membership condition for one query field.
 *
 * @param <T> the field value type
 * @author RollW
 */
public final class QueryCondition<T> implements QueryExpression {
    private final QueryField<T> field;
    private final QueryOperator operator;
    private final List<T> values;

    private QueryCondition(QueryField<T> field, QueryOperator operator, List<T> values) {
        this.field = field;
        this.operator = operator;
        this.values = values;
    }

    /**
     * Creates an equality condition.
     *
     * @param field the typed domain field
     * @param value the required comparison value
     * @param <T>   the field value type
     * @return the immutable condition
     */
    public static <T> QueryCondition<T> equal(QueryField<T> field, T value) {
        return singleValue(field, QueryOperator.EQUAL, value);
    }

    /**
     * Creates a non-equality condition.
     *
     * @param field the typed domain field
     * @param value the required comparison value
     * @param <T>   the field value type
     * @return the immutable condition
     */
    public static <T> QueryCondition<T> notEqual(QueryField<T> field, T value) {
        return singleValue(field, QueryOperator.NOT_EQUAL, value);
    }

    /**
     * Creates a membership condition.
     *
     * @param field  the typed domain field
     * @param values the non-empty required candidate values
     * @param <T>    the field value type
     * @return the immutable condition
     */
    public static <T> QueryCondition<T> in(QueryField<T> field, Collection<? extends T> values) {
        return multipleValues(field, QueryOperator.IN, values);
    }

    /**
     * Creates a negative membership condition.
     *
     * @param field  the typed domain field
     * @param values the non-empty required candidate values
     * @param <T>    the field value type
     * @return the immutable condition
     */
    public static <T> QueryCondition<T> notIn(QueryField<T> field, Collection<? extends T> values) {
        return multipleValues(field, QueryOperator.NOT_IN, values);
    }

    /**
     * Returns the condition field.
     *
     * @return the typed domain field
     */
    public QueryField<T> getField() {
        return field;
    }

    /**
     * Returns the equality or membership operator.
     *
     * @return the condition operator
     */
    public QueryOperator getOperator() {
        return operator;
    }

    /**
     * Returns immutable values with the same type as the field.
     *
     * @return the comparison values
     */
    public List<T> getValues() {
        return values;
    }

    @Override
    public <R> R accept(QueryExpressionVisitor<R> visitor) {
        return Objects.requireNonNull(visitor, "visitor").visitCondition(this);
    }

    private static <T> QueryCondition<T> singleValue(QueryField<T> field, QueryOperator operator, T value) {
        QueryField<T> nonNullField = Objects.requireNonNull(field, "field");
        requireSimpleOperator(operator);
        nonNullField.requireValue(value);
        return new QueryCondition<>(nonNullField, operator, List.of(value));
    }

    private static <T> QueryCondition<T> multipleValues(
            QueryField<T> field,
            QueryOperator operator,
            Collection<? extends T> values
    ) {
        QueryField<T> nonNullField = Objects.requireNonNull(field, "field");
        requireMembershipOperator(operator);
        Objects.requireNonNull(values, "values");
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Membership values must not be empty");
        }
        List<T> validatedValues = new ArrayList<>(values.size());
        for (T value : values) {
            nonNullField.requireValue(value);
            validatedValues.add(value);
        }
        return new QueryCondition<>(nonNullField, operator, List.copyOf(validatedValues));
    }

    private static void requireSimpleOperator(QueryOperator operator) {
        if (operator != QueryOperator.EQUAL && operator != QueryOperator.NOT_EQUAL) {
            throw new IllegalArgumentException("Operator does not accept one equality value");
        }
    }

    private static void requireMembershipOperator(QueryOperator operator) {
        if (operator != QueryOperator.IN && operator != QueryOperator.NOT_IN) {
            throw new IllegalArgumentException("Operator does not accept membership values");
        }
    }
}
