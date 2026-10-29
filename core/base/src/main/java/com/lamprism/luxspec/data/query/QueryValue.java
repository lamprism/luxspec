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
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A typed value expression that may be used by a {@link QueryValueCondition}.
 *
 * <p>Application functions use typed {@link QueryFunctionDefinition} identities. Query schemas and
 * executors separately authorize and implement those identities; a function name is never an SQL
 * fragment.</p>
 *
 * <p>Application-owned executors may interpret additional value nodes. The standard schema and
 * executors reject nodes outside the built-in model.</p>
 *
 * @param <T> the expression result type
 *
 * @author RollW
 */
public interface QueryValue<T> {
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
     * Applies an application-owned typed function definition.
     *
     * @param definition the function identity and signature
     * @param arguments  the ordered argument expressions
     * @param <T>        the result type
     * @return the function value expression
     */
    static <T> QueryValue<T> function(QueryFunctionDefinition<T> definition, QueryValue<?>... arguments) {
        return new CustomFunction<>(definition, List.of(Objects.requireNonNull(arguments, "arguments")));
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
        return aggregate(QueryAggregateFunction.COUNT, Long.class, null);
    }

    /**
     * Creates an application-defined aggregate over the supplied scalar expressions.
     *
     * @param definition the typed aggregate identity
     * @param arguments  the ordered per-row operand expressions
     * @param <T>        the aggregate result type
     * @return the aggregate value
     */
    static <T> QueryValue<T> aggregate(QueryAggregateDefinition<T> definition, QueryValue<?>... arguments) {
        QueryAggregateDefinition<T> nonNullDefinition = Objects.requireNonNull(definition, "definition");
        return new Aggregate<>(nonNullDefinition, nonNullDefinition.getResultType(),
                List.of(Objects.requireNonNull(arguments, "arguments")));
    }

    /**
     * Creates a non-null-value count aggregate.
     *
     * @param value the counted value
     * @param <T>   the counted value type
     * @return the count aggregate
     */
    static <T> QueryValue<Long> count(QueryValue<T> value) {
        return aggregate(QueryAggregateFunction.COUNT, Long.class, Objects.requireNonNull(value, "value"));
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
     * Reports whether a built-in value expression contains an aggregate node.
     *
     * <p>Application-owned nodes are not traversed and require their own validation.</p>
     *
     * @param value the value expression to inspect
     * @return true when a built-in node contains an aggregate
     */
    static boolean containsAggregate(QueryValue<?> value) {
        Deque<QueryValue<?>> pending = new ArrayDeque<>();
        Set<QueryValue<?>> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        pending.push(Objects.requireNonNull(value, "value"));
        while (!pending.isEmpty()) {
            QueryValue<?> current = pending.pop();
            if (!visited.add(current)) {
                continue;
            }
            if (current instanceof QueryValue.Aggregate<?>) {
                return true;
            }
            if (current instanceof QueryValue.Function<?> function) {
                pending.addAll(function.getArguments());
            } else if (current instanceof QueryValue.CustomFunction<?> function) {
                pending.addAll(function.getArguments());
            } else if (current instanceof QueryValue.Cast<?> cast) {
                pending.push(cast.getOperand());
            }
        }
        return false;
    }

    /**
     * Reports whether a node belongs to the standard value model.
     *
     * @param value the value node to inspect
     * @return true for a built-in node
     */
    static boolean isBuiltIn(QueryValue<?> value) {
        QueryValue<?> node = Objects.requireNonNull(value, "value");
        return node instanceof Field<?> || node instanceof Literal<?> || node instanceof Function<?>
                || node instanceof CustomFunction<?> || node instanceof Cast<?> || node instanceof Aggregate<?>;
    }

    /**
     * Dispatches this value to a typed visitor.
     *
     * <p>Application-owned nodes may use their own dispatch protocol. The default rejects the
     * built-in visitor because it does not define application-specific operations.</p>
     *
     * @param visitor the value visitor
     * @param <R>     the visitor result type
     * @return the visitor result
     */
    default <R> R accept(QueryValueVisitor<R> visitor) {
        Objects.requireNonNull(visitor, "visitor");
        throw new UnsupportedOperationException("Application query value does not support the standard visitor");
    }

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
            QueryAggregateFunction.Standard function,
            Class<T> valueType,
            @Nullable QueryValue<?> value
    ) {
        return new Aggregate<>(function, valueType, value == null ? List.of() : List.of(value));
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
     * A value produced by an application-owned function definition.
     *
     * @param <T> the result type
     */
    final class CustomFunction<T> implements QueryValue<T> {
        private final QueryFunctionDefinition<T> definition;
        private final List<QueryValue<?>> arguments;

        private CustomFunction(QueryFunctionDefinition<T> definition, List<QueryValue<?>> arguments) {
            this.definition = Objects.requireNonNull(definition, "definition");
            this.arguments = List.copyOf(arguments);
            List<Class<?>> types = definition.getArgumentTypes();
            if (types.size() != this.arguments.size()) {
                throw new IllegalArgumentException("Query function argument count does not match its signature");
            }
            for (int index = 0; index < types.size(); index++) {
                if (!types.get(index).isAssignableFrom(this.arguments.get(index).getValueType())) {
                    throw new IllegalArgumentException("Query function argument type does not match its signature");
                }
            }
        }

        /**
         * @return the registered function definition
         */
        public QueryFunctionDefinition<T> getDefinition() {
            return definition;
        }

        /**
         * @return the immutable ordered argument expressions
         */
        public List<QueryValue<?>> getArguments() {
            return arguments;
        }

        @Override
        public Class<T> getValueType() {
            return definition.getResultType();
        }

        @Override
        public <R> R accept(QueryValueVisitor<R> visitor) {
            return Objects.requireNonNull(visitor, "visitor").visitCustomFunction(this);
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
     * A standard or application-defined aggregate value.
     *
     * @param <T> the aggregate result type
     */
    final class Aggregate<T> implements QueryValue<T> {
        private final QueryAggregateFunction function;
        private final Class<T> valueType;
        private final List<QueryValue<?>> arguments;

        private Aggregate(
                QueryAggregateFunction function,
                Class<T> valueType,
                List<QueryValue<?>> arguments
        ) {
            this.function = Objects.requireNonNull(function, "function");
            this.valueType = requireReferenceType(valueType, "valueType");
            this.arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
            if (function instanceof QueryAggregateFunction.Standard standard) {
                if ((standard == QueryAggregateFunction.COUNT && this.arguments.size() > 1)
                        || (standard != QueryAggregateFunction.COUNT && this.arguments.size() != 1)) {
                    throw new IllegalArgumentException("Invalid standard aggregate operand count");
                }
            } else if (function instanceof QueryAggregateDefinition<?> definition) {
                List<Class<?>> types = List.copyOf(definition.getArgumentTypes());
                if (!valueType.equals(definition.getResultType()) || types.size() != this.arguments.size()) {
                    throw new IllegalArgumentException("Aggregate signature does not match its operands");
                }
                for (int index = 0; index < types.size(); index++) {
                    Class<?> type = requireReferenceType(types.get(index), "aggregate argument type");
                    if (!type.isAssignableFrom(this.arguments.get(index).getValueType())) {
                        throw new IllegalArgumentException("Aggregate operand type does not match its signature");
                    }
                }
            } else {
                throw new IllegalArgumentException("Aggregate must use a typed definition");
            }
            for (QueryValue<?> argument : this.arguments) {
                if (containsAggregate(argument)) {
                    throw new IllegalArgumentException("Aggregate operands must not contain aggregates");
                }
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
         * Returns the ordered per-row operand expressions.
         *
         * @return the immutable operands, which are empty for a row-only aggregate
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
            return Objects.requireNonNull(visitor, "visitor").visitAggregate(this);
        }
    }
}
