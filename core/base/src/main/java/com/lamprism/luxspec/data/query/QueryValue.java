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

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * A typed value expression that may be used by a {@link QueryValueCondition}.
 *
 * <p>Only fields, literals, the closed {@link QueryFunction} set, and typed casts can be
 * represented. This keeps provider adapters in control of their expression mapping without
 * accepting arbitrary function names or SQL fragments from a caller.</p>
 *
 * @param <T> the expression result type
 * @author RollW
 */
public sealed interface QueryValue<T>
        permits QueryValue.Field, QueryValue.Literal, QueryValue.Function, QueryValue.Cast, QueryValue.Aggregate {
    /**
     * Wraps a typed query field.
     *
     * @param field the query field
     * @param <T>   the field value type
     * @return the field value expression
     */
    static <T> QueryValue<T> field(QueryField<T> field) {
        return new Field<>(field);
    }

    /**
     * Creates a non-null literal value expression.
     *
     * @param value the literal value
     * @param <T>   the literal type
     * @return the literal expression
     */
    static <T> QueryValue<T> literal(T value) {
        T nonNullValue = Objects.requireNonNull(value, "value");
        @SuppressWarnings("unchecked")
        Class<T> valueType = (Class<T>) nonNullValue.getClass();
        return literal(valueType, nonNullValue);
    }

    /**
     * Creates a non-null literal with an explicit declared type.
     *
     * @param valueType the non-primitive declared type
     * @param value     the literal value
     * @param <T>       the literal type
     * @return the literal expression
     */
    static <T> QueryValue<T> literal(Class<T> valueType, T value) {
        Class<T> nonNullValueType = requireReferenceType(valueType, "valueType");
        T nonNullValue = Objects.requireNonNull(value, "value");
        if (!nonNullValueType.isInstance(nonNullValue)) {
            throw new IllegalArgumentException("Literal value does not match its declared type");
        }
        return new Literal<>(nonNullValueType, nonNullValue);
    }

    /**
     * Creates a lower-case transformation for a string value.
     *
     * @param value the string value expression
     * @return the lower-case expression
     */
    static QueryValue<String> lower(QueryValue<String> value) {
        return function(QueryFunction.LOWER, String.class, value);
    }

    /**
     * Creates a lower-case transformation for a string field.
     *
     * @param field the string field
     * @return the lower-case expression
     */
    static QueryValue<String> lower(QueryField<String> field) {
        return lower(field(field));
    }

    /**
     * Creates an upper-case transformation for a string value.
     *
     * @param value the string value expression
     * @return the upper-case expression
     */
    static QueryValue<String> upper(QueryValue<String> value) {
        return function(QueryFunction.UPPER, String.class, value);
    }

    /**
     * Creates an upper-case transformation for a string field.
     *
     * @param field the string field
     * @return the upper-case expression
     */
    static QueryValue<String> upper(QueryField<String> field) {
        return upper(field(field));
    }

    /**
     * Creates a two-value first-non-null expression.
     *
     * @param value    the primary expression
     * @param fallback the fallback value
     * @param <T>      the expression type
     * @return the coalesce expression
     */
    static <T> QueryValue<T> coalesce(QueryValue<T> value, T fallback) {
        QueryValue<T> nonNullValue = Objects.requireNonNull(value, "value");
        T nonNullFallback = Objects.requireNonNull(fallback, "fallback");
        QueryValue<T> fallbackValue = literal(nonNullValue.getValueType(), nonNullFallback);
        return function(QueryFunction.COALESCE, nonNullValue.getValueType(), nonNullValue, fallbackValue);
    }

    /**
     * Creates a two-value first-non-null expression for a field.
     *
     * @param field    the primary field
     * @param fallback the fallback value
     * @param <T>      the expression type
     * @return the coalesce expression
     */
    static <T> QueryValue<T> coalesce(QueryField<T> field, T fallback) {
        return coalesce(field(field), fallback);
    }

    /**
     * Creates a two-value first-non-null expression.
     *
     * @param first  the primary expression
     * @param second the fallback expression
     * @param <T>    the expression type
     * @return the coalesce expression
     */
    static <T> QueryValue<T> coalesce(QueryValue<T> first, QueryValue<T> second) {
        QueryValue<T> nonNullFirst = Objects.requireNonNull(first, "first");
        QueryValue<T> nonNullSecond = Objects.requireNonNull(second, "second");
        return function(QueryFunction.COALESCE, nonNullFirst.getValueType(), nonNullFirst, nonNullSecond);
    }

    /**
     * Creates a typed cast expression.
     *
     * @param value      the source expression
     * @param targetType the non-primitive target type
     * @param <T>        the source type
     * @param <R>        the target type
     * @return the cast expression
     */
    static <T, R> QueryValue<R> cast(QueryValue<T> value, Class<R> targetType) {
        return new Cast<>(value, targetType);
    }

    /**
     * Creates a typed cast expression for a field.
     *
     * @param field      the source field
     * @param targetType the non-primitive target type
     * @param <T>        the source type
     * @param <R>        the target type
     * @return the cast expression
     */
    static <T, R> QueryValue<R> cast(QueryField<T> field, Class<R> targetType) {
        return cast(field(field), targetType);
    }

    /**
     * Creates a row-count aggregate.
     *
     * @return the count aggregate
     */
    static QueryValue<Long> count() {
        return new Aggregate<>(QueryAggregateFunction.COUNT, Long.class, null);
    }

    /**
     * Creates a non-null-value count aggregate.
     *
     * @param value the counted value
     * @param <T>   the counted value type
     * @return the count aggregate
     */
    static <T> QueryValue<Long> count(QueryValue<T> value) {
        return aggregate(QueryAggregateFunction.COUNT, Long.class, value);
    }

    /**
     * Creates a distinct-value count aggregate.
     *
     * @param value the counted value
     * @param <T>   the counted value type
     * @return the distinct count aggregate
     */
    static <T> QueryValue<Long> countDistinct(QueryValue<T> value) {
        return aggregate(QueryAggregateFunction.COUNT_DISTINCT, Long.class, value);
    }

    /**
     * Creates a numeric sum aggregate.
     *
     * @param value the numeric value
     * @param <T>   the numeric value type
     * @return the sum as a decimal
     */
    static <T extends Number> QueryValue<BigDecimal> sum(QueryValue<T> value) {
        return aggregate(QueryAggregateFunction.SUM, BigDecimal.class, value);
    }

    /**
     * Creates a numeric average aggregate.
     *
     * @param value the numeric value
     * @param <T>   the numeric value type
     * @return the average as a double
     */
    static <T extends Number> QueryValue<Double> average(QueryValue<T> value) {
        return aggregate(QueryAggregateFunction.AVERAGE, Double.class, value);
    }

    /**
     * Creates a minimum aggregate.
     *
     * @param value the comparable value
     * @param <T>   the value type
     * @return the minimum aggregate
     */
    static <T extends Comparable<? super T>> QueryValue<T> min(QueryValue<T> value) {
        return aggregate(QueryAggregateFunction.MIN, value.getValueType(), value);
    }

    /**
     * Creates a maximum aggregate.
     *
     * @param value the comparable value
     * @param <T>   the value type
     * @return the maximum aggregate
     */
    static <T extends Comparable<? super T>> QueryValue<T> max(QueryValue<T> value) {
        return aggregate(QueryAggregateFunction.MAX, value.getValueType(), value);
    }

    /**
     * Returns the Java result type exposed by this expression.
     *
     * @return the expression type
     */
    Class<T> getValueType();

    /**
     * Reports whether this expression contains an aggregate node.
     *
     * @param value the value expression to inspect
     * @return true when the expression contains an aggregate
     */
    static boolean containsAggregate(QueryValue<?> value) {
        QueryValue<?> nonNullValue = Objects.requireNonNull(value, "value");
        if (nonNullValue instanceof QueryValue.Aggregate<?>) {
            return true;
        }
        if (nonNullValue instanceof QueryValue.Function<?> function) {
            for (QueryValue<?> argument : function.getArguments()) {
                if (containsAggregate(argument)) {
                    return true;
                }
            }
        }
        if (nonNullValue instanceof QueryValue.Cast<?> cast) {
            return containsAggregate(cast.getOperand());
        }
        return false;
    }

    /**
     * Dispatches this value to a typed visitor.
     *
     * @param visitor the value visitor
     * @param <R>     the visitor result type
     * @return the visitor result
     */
    <R> R accept(QueryValueVisitor<R> visitor);

    private static <T> Class<T> requireReferenceType(Class<T> type, String name) {
        Class<T> nonNullType = Objects.requireNonNull(type, name);
        if (nonNullType.isPrimitive()) {
            throw new IllegalArgumentException(name + " must use a boxed or reference type");
        }
        return nonNullType;
    }

    private static <T> QueryValue<T> function(
            QueryFunction function,
            Class<T> valueType,
            QueryValue<?>... arguments
    ) {
        return new Function<>(function, valueType, List.of(arguments));
    }

    private static <T> QueryValue<T> aggregate(
            QueryAggregateFunction function,
            Class<T> valueType,
            @Nullable QueryValue<?> value
    ) {
        return new Aggregate<>(function, valueType, value);
    }

    /**
     * A direct query-field value.
     *
     * @param <T> the field value type
     */
    final class Field<T> implements QueryValue<T> {
        private final QueryField<T> field;

        private Field(QueryField<T> field) {
            this.field = Objects.requireNonNull(field, "field");
        }

        /**
         * Returns the wrapped field.
         *
         * @return the query field
         */
        public QueryField<T> getField() {
            return field;
        }

        @Override
        public Class<T> getValueType() {
            return field.getValueType();
        }

        @Override
        public <R> R accept(QueryValueVisitor<R> visitor) {
            return Objects.requireNonNull(visitor, "visitor").visitField(field);
        }
    }

    /**
     * A non-null literal value.
     *
     * @param <T> the literal type
     */
    final class Literal<T> implements QueryValue<T> {
        private final Class<T> valueType;
        private final T value;

        private Literal(Class<T> valueType, T value) {
            this.valueType = Objects.requireNonNull(valueType, "valueType");
            this.value = Objects.requireNonNull(value, "value");
        }

        /**
         * Returns the literal value.
         *
         * @return the non-null value
         */
        public T getValue() {
            return value;
        }

        @Override
        public Class<T> getValueType() {
            return valueType;
        }

        @Override
        public <R> R accept(QueryValueVisitor<R> visitor) {
            return Objects.requireNonNull(visitor, "visitor").visitLiteral(this);
        }
    }

    /**
     * A value produced by one closed query function.
     *
     * @param <T> the function result type
     */
    final class Function<T> implements QueryValue<T> {
        private final QueryFunction function;
        private final Class<T> valueType;
        private final List<QueryValue<?>> arguments;

        private Function(QueryFunction function, Class<T> valueType, List<QueryValue<?>> arguments) {
            this.function = Objects.requireNonNull(function, "function");
            this.valueType = requireReferenceType(valueType, "valueType");
            this.arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
            validateArguments();
        }

        /**
         * Returns the closed function name.
         *
         * @return the function
         */
        public QueryFunction getFunction() {
            return function;
        }

        /**
         * Returns immutable function arguments.
         *
         * @return the function arguments
         */
        public List<QueryValue<?>> getArguments() {
            return arguments;
        }

        @Override
        public Class<T> getValueType() {
            return valueType;
        }

        @Override
        public <R> R accept(QueryValueVisitor<R> visitor) {
            return Objects.requireNonNull(visitor, "visitor").visitFunction(this);
        }

        private void validateArguments() {
            switch (function) {
                case LOWER, UPPER -> {
                    requireArgumentCount(1);
                    if (arguments.get(0).getValueType() != String.class || valueType != String.class) {
                        throw new IllegalArgumentException(function + " requires a String value");
                    }
                }
                case COALESCE -> {
                    requireArgumentCount(2);
                    Class<?> firstType = arguments.get(0).getValueType();
                    Class<?> secondType = arguments.get(1).getValueType();
                    if (!firstType.isAssignableFrom(secondType) || valueType != firstType) {
                        throw new IllegalArgumentException("COALESCE arguments must have compatible types");
                    }
                }
                case CAST -> throw new IllegalArgumentException("CAST is represented by a cast value");
            }
        }

        private void requireArgumentCount(int expected) {
            if (arguments.size() != expected) {
                throw new IllegalArgumentException(function + " requires " + expected + " argument(s)");
            }
        }
    }

    /**
     * A typed cast of another value expression.
     *
     * @param <T> the target type
     */
    final class Cast<T> implements QueryValue<T> {
        private final QueryValue<?> operand;
        private final Class<T> targetType;

        private Cast(QueryValue<?> operand, Class<T> targetType) {
            this.operand = Objects.requireNonNull(operand, "operand");
            this.targetType = requireReferenceType(targetType, "targetType");
        }

        /**
         * Returns the source expression.
         *
         * @return the source expression
         */
        public QueryValue<?> getOperand() {
            return operand;
        }

        /**
         * Returns the cast target type.
         *
         * @return the target type
         */
        public Class<T> getTargetType() {
            return targetType;
        }

        @Override
        public Class<T> getValueType() {
            return targetType;
        }

        @Override
        public <R> R accept(QueryValueVisitor<R> visitor) {
            return Objects.requireNonNull(visitor, "visitor").visitCast(this);
        }
    }

    /**
     * An allowlisted aggregate value.
     *
     * @param <T> the aggregate result type
     */
    final class Aggregate<T> implements QueryValue<T> {
        private final QueryAggregateFunction function;
        private final Class<T> valueType;
        private final @Nullable QueryValue<?> operand;

        private Aggregate(
                QueryAggregateFunction function,
                Class<T> valueType,
                @Nullable QueryValue<?> operand
        ) {
            this.function = Objects.requireNonNull(function, "function");
            this.valueType = requireReferenceType(valueType, "valueType");
            if (function == QueryAggregateFunction.COUNT && operand == null) {
                this.operand = null;
            } else {
                this.operand = Objects.requireNonNull(operand, "operand");
            }
        }

        /**
         * Returns the aggregate function.
         *
         * @return the aggregate function
         */
        public QueryAggregateFunction getFunction() {
            return function;
        }

        /**
         * Returns the optional aggregate operand.
         *
         * @return the operand, or null for row count
         */
        public @Nullable QueryValue<?> getOperand() {
            return operand;
        }

        @Override
        public Class<T> getValueType() {
            return valueType;
        }

        @Override
        public <R> R accept(QueryValueVisitor<R> visitor) {
            return Objects.requireNonNull(visitor, "visitor").visitAggregate(this);
        }
    }
}
