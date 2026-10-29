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
 * A typed operator condition for one query field.
 *
 * @param <T> the field value type
 * @param <A> the operator argument type
 *
 * @author RollW
 */
public final class QueryCondition<T, A> implements QueryExpression {
    private final QueryField<T> field;
    private final QueryOperator operator;
    private final List<A> values;

    private QueryCondition(QueryField<T> field, QueryOperator operator, List<A> values) {
        this.field = field;
        this.operator = operator;
        this.values = values;
    }

    /**
     * Creates a condition using an application-defined operator.
     *
     * @param field     the typed domain field
     * @param operator  the typed operation definition
     * @param arguments the operation arguments
     * @param <T>       the field value type
     * @param <A>       the operation argument type
     * @return the immutable condition
     */
    public static <T, A> QueryCondition<T, A> of(
            QueryField<T> field, QueryOperatorDefinition<? super T, A> operator, Collection<? extends A> arguments
    ) {
        QueryField<T> nonNullField = Objects.requireNonNull(field, "field");
        return new QueryCondition<>(nonNullField, operator,
                QueryOperatorArguments.validate(nonNullField.getValueType(), operator, arguments));
    }

    /**
     * Creates an equality condition.
     *
     * @param field the typed domain field
     * @param value the required comparison value
     * @param <T>   the field value type
     * @return the immutable condition
     */
    public static <T> QueryCondition<T, T> equal(QueryField<T> field, T value) {
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
    public static <T> QueryCondition<T, T> notEqual(QueryField<T> field, T value) {
        return singleValue(field, QueryOperator.NOT_EQUAL, value);
    }

    /**
     * Creates a null-value condition.
     *
     * @param field the typed domain field
     * @param <T>   the field value type
     * @return the immutable null-value condition
     */
    public static <T> QueryCondition<T, T> isNull(QueryField<T> field) {
        return noValue(field, QueryOperator.IS_NULL);
    }

    /**
     * Creates a non-null-value condition.
     *
     * @param field the typed domain field
     * @param <T>   the field value type
     * @return the immutable non-null-value condition
     */
    public static <T> QueryCondition<T, T> isNotNull(QueryField<T> field) {
        return noValue(field, QueryOperator.IS_NOT_NULL);
    }

    /**
     * Creates a membership condition.
     *
     * @param field  the typed domain field
     * @param values the non-empty required candidate values
     * @param <T>    the field value type
     * @return the immutable condition
     */
    public static <T> QueryCondition<T, T> in(QueryField<T> field, Collection<? extends T> values) {
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
    public static <T> QueryCondition<T, T> notIn(QueryField<T> field, Collection<? extends T> values) {
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
     * Returns the condition operator.
     *
     * @return the condition operator
     */
    public QueryOperator getOperator() {
        return operator;
    }

    /**
     * Returns immutable argument values. Their type is defined by the operator; null-value
     * conditions have no arguments.
     *
     * @return the comparison values
     */
    public List<A> getValues() {
        return values;
    }

    @Override
    public <R> R accept(QueryExpressionVisitor<R> visitor) {
        return Objects.requireNonNull(visitor, "visitor").visitCondition(this);
    }

    private static <T> QueryCondition<T, T> singleValue(QueryField<T> field, QueryOperator operator, T value) {
        QueryField<T> nonNullField = Objects.requireNonNull(field, "field");
        requireSimpleOperator(operator);
        nonNullField.requireValue(value);
        return new QueryCondition<>(nonNullField, operator, List.of(value));
    }

    private static <T> QueryCondition<T, T> noValue(QueryField<T> field, QueryOperator operator) {
        QueryField<T> nonNullField = Objects.requireNonNull(field, "field");
        if (operator != QueryOperator.IS_NULL && operator != QueryOperator.IS_NOT_NULL) {
            throw new IllegalArgumentException("Operator does not accept an empty value list");
        }
        return new QueryCondition<>(nonNullField, operator, List.of());
    }

    private static <T> QueryCondition<T, T> multipleValues(
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
