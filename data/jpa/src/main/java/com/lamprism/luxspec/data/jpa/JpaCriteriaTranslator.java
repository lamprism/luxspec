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

package com.lamprism.luxspec.data.jpa;

import com.lamprism.luxspec.data.pagination.KeysetPosition;
import com.lamprism.luxspec.data.query.ComparisonCondition;
import com.lamprism.luxspec.data.query.ExistsCondition;
import com.lamprism.luxspec.data.query.LikeCondition;
import com.lamprism.luxspec.data.query.LogicalCondition;
import com.lamprism.luxspec.data.query.LogicalOperator;
import com.lamprism.luxspec.data.query.NegatedQueryExpression;
import com.lamprism.luxspec.data.query.OrderBy;
import com.lamprism.luxspec.data.query.QueryAggregateDefinition;
import com.lamprism.luxspec.data.query.QueryAggregateFunction;
import com.lamprism.luxspec.data.query.QueryCondition;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.data.query.QueryExpression;
import com.lamprism.luxspec.data.query.QueryExpressionVisitor;
import com.lamprism.luxspec.data.query.QueryField;
import com.lamprism.luxspec.data.query.QueryFunctionDefinition;
import com.lamprism.luxspec.data.query.QueryOperator;
import com.lamprism.luxspec.data.query.QueryOperatorDefinition;
import com.lamprism.luxspec.data.query.QueryValue;
import com.lamprism.luxspec.data.query.QueryValueCondition;
import com.lamprism.luxspec.data.query.QueryValueVisitor;
import jakarta.persistence.criteria.CommonAbstractCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Translates the typed Luxspec structured-query model into JPA Criteria API objects.
 *
 * @author RollW
 */
public class JpaCriteriaTranslator {
    private final Map<QueryFunctionDefinition<?>, JpaQueryFunction<?>> functions;
    private final Map<QueryOperatorDefinition<?, ?>, JpaQueryOperator<?, ?>> operators;
    private final Map<QueryAggregateDefinition<?>, JpaQueryAggregate<?>> aggregates;

    /**
     * Creates a stateless typed Criteria API translator.
     */
    public JpaCriteriaTranslator() {
        this(Map.of(), Map.of(), Map.of());
    }

    private JpaCriteriaTranslator(Map<QueryFunctionDefinition<?>, JpaQueryFunction<?>> functions,
                                  Map<QueryOperatorDefinition<?, ?>, JpaQueryOperator<?, ?>> operators,
                                  Map<QueryAggregateDefinition<?>, JpaQueryAggregate<?>> aggregates) {
        this.functions = Map.copyOf(functions);
        this.operators = Map.copyOf(operators);
        this.aggregates = Map.copyOf(aggregates);
    }

    /**
     * Starts a translator with application-owned function and operator implementations.
     *
     * @return the translator builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Configures trusted Criteria implementations of query functions and operators.
     */
    public static final class Builder {
        private final Map<QueryFunctionDefinition<?>, JpaQueryFunction<?>> functions = new LinkedHashMap<>();
        private final Map<QueryOperatorDefinition<?, ?>, JpaQueryOperator<?, ?>> operators = new LinkedHashMap<>();
        private final Map<QueryAggregateDefinition<?>, JpaQueryAggregate<?>> aggregates = new LinkedHashMap<>();

        private Builder() {
        }

        /**
         * Registers a typed function implementation.
         *
         * @param definition     the function identity
         * @param implementation the trusted Criteria translation
         * @param <V>            the function result type
         * @return this builder
         */
        public <V> Builder function(QueryFunctionDefinition<V> definition, JpaQueryFunction<V> implementation) {
            QueryFunctionDefinition<V> key = Objects.requireNonNull(definition, "definition");
            if (functions.putIfAbsent(key, Objects.requireNonNull(implementation, "implementation")) != null) {
                throw new IllegalArgumentException("Query function is already registered: " + key.getName());
            }
            return this;
        }

        /**
         * Registers a trusted Criteria implementation of a typed query operator.
         *
         * @param definition     the operation identity
         * @param implementation the Criteria translation
         * @param <V>            the operand type
         * @param <A>            the argument type
         * @return this builder
         */
        public <V, A> Builder operator(
                QueryOperatorDefinition<V, A> definition, JpaQueryOperator<V, A> implementation
        ) {
            QueryOperatorDefinition<V, A> key = Objects.requireNonNull(definition, "definition");
            if (operators.putIfAbsent(key, Objects.requireNonNull(implementation, "implementation")) != null) {
                throw new IllegalArgumentException("Query operator is already registered: " + key.getName());
            }
            return this;
        }

        /**
         * Registers a trusted Criteria implementation of a typed group aggregate.
         *
         * @param definition     the aggregate identity
         * @param implementation the Criteria translation
         * @param <V>            the aggregate result type
         * @return this builder
         */
        public <V> Builder aggregate(QueryAggregateDefinition<V> definition, JpaQueryAggregate<V> implementation) {
            QueryAggregateDefinition<V> key = Objects.requireNonNull(definition, "definition");
            if (aggregates.putIfAbsent(key, Objects.requireNonNull(implementation, "implementation")) != null) {
                throw new IllegalArgumentException("Query aggregate is already registered: " + key.getName());
            }
            return this;
        }

        /**
         * Builds an immutable translator.
         *
         * @return the configured translator
         */
        public JpaCriteriaTranslator build() {
            return new JpaCriteriaTranslator(functions, operators, aggregates);
        }
    }

    /**
     * Creates a predicate using the default direct same-name field resolver.
     *
     * @param builder  the JPA criteria builder
     * @param root     the query entity root
     * @param criteria structured conditions and ordering
     * @param <T>      the entity type
     * @return the translated predicate
     */
    public <T> Predicate predicate(
            CriteriaBuilder builder,
            Root<T> root,
            QueryCriteria criteria
    ) {
        return predicate(builder, root, criteria, JpaQueryFieldResolver.byAttributeName());
    }

    /**
     * Creates a predicate using a direct attribute resolver with a naming strategy.
     *
     * @param builder        the JPA criteria builder
     * @param root           the query entity root
     * @param criteria       structured conditions and ordering
     * @param namingStrategy the query-to-attribute naming strategy
     * @param <T>            the entity type
     * @return the translated predicate
     */
    public <T> Predicate predicate(
            CriteriaBuilder builder,
            Root<T> root,
            QueryCriteria criteria,
            JpaFieldNamingStrategy namingStrategy
    ) {
        return predicate(builder, root, criteria, JpaQueryFieldResolver.byAttributeName(namingStrategy));
    }

    /**
     * Creates a predicate for the complete structured filter expression.
     *
     * <p>This overload is an advanced mapping extension point. Use the overload without a
     * resolver for direct same-name attributes.</p>
     *
     * @param builder       the JPA criteria builder
     * @param root          the query entity root
     * @param criteria      structured conditions and ordering
     * @param fieldResolver explicit allowed typed field resolver
     * @param <T>           the entity type
     * @return the translated predicate
     */
    public <T> Predicate predicate(
            CriteriaBuilder builder,
            Root<T> root,
            QueryCriteria criteria,
            JpaQueryFieldResolver<T> fieldResolver
    ) {
        return predicate(builder, root, null, criteria.getCondition(), fieldResolver);
    }

    /**
     * Creates a predicate for one expression with a Criteria query context.
     *
     * <p>The context is required when the expression contains an {@code EXISTS} relation
     * predicate because JPA subqueries belong to a criteria query.</p>
     *
     * @param builder       the JPA criteria builder
     * @param root          the query entity root
     * @param query         the owning criteria query or subquery
     * @param expression    the structured expression
     * @param fieldResolver explicit allowed typed field resolver
     * @param <T>           the entity type
     * @return the translated predicate
     */
    public <T> Predicate predicate(
            CriteriaBuilder builder,
            Root<T> root,
            @Nullable CommonAbstractCriteria query,
            QueryExpression expression,
            JpaQueryFieldResolver<T> fieldResolver
    ) {
        return new PredicateTranslator<>(builder, root, query, fieldResolver, functions, operators, aggregates)
                .translate(expression);
    }

    /**
     * Creates a JPA expression for one typed query value.
     *
     * @param builder       the JPA criteria builder
     * @param root          the query entity root
     * @param query         the owning criteria query or subquery
     * @param value         the typed value expression
     * @param fieldResolver explicit allowed typed field resolver
     * @param <T>           the entity type
     * @return the translated value expression
     */
    public <T> Expression<?> value(
            CriteriaBuilder builder,
            Root<T> root,
            @Nullable CommonAbstractCriteria query,
            QueryValue<?> value,
            JpaQueryFieldResolver<T> fieldResolver
    ) {
        return new PredicateTranslator<>(builder, root, query, fieldResolver, functions, operators, aggregates)
                .expression(value);
    }

    /**
     * Creates the lexicographic continuation predicate for a keyset position.
     *
     * <p>Each branch compares one order field after all preceding fields are equal. The caller
     * must supply an explicit stable ordering and a position with one value per order term.</p>
     *
     * @param builder  the JPA criteria builder
     * @param root     the query entity root
     * @param criteria the structured query criteria
     * @param position the typed keyset position
     * @param resolver the explicit field resolver
     * @param <T>      the entity type
     * @return the lexicographic continuation predicate
     */
    public <T> Predicate keysetPredicate(
            CriteriaBuilder builder,
            Root<T> root,
            QueryCriteria criteria,
            KeysetPosition position,
            JpaQueryFieldResolver<T> resolver
    ) {
        CriteriaBuilder nonNullBuilder = Objects.requireNonNull(builder, "builder");
        Root<T> nonNullRoot = Objects.requireNonNull(root, "root");
        QueryCriteria nonNullCriteria = Objects.requireNonNull(criteria, "criteria");
        KeysetPosition nonNullPosition = Objects.requireNonNull(position, "position");
        JpaQueryFieldResolver<T> nonNullResolver = Objects.requireNonNull(resolver, "resolver");
        List<OrderBy<?>> orders = nonNullCriteria.getOrders();
        List<@Nullable Object> values = nonNullPosition.values();
        if (orders.isEmpty()) {
            throw new IllegalArgumentException("Keyset windows require explicit query ordering");
        }
        if (values.size() != orders.size()) {
            throw new IllegalArgumentException("Keyset position does not match query ordering");
        }
        for (int index = 0; index < orders.size(); index++) {
            Object value = values.get(index);
            if (value != null && !orders.get(index).getField().getValueType().isInstance(value)) {
                throw new IllegalArgumentException("Keyset value does not match query field type");
            }
        }

        List<Predicate> branches = new ArrayList<>(orders.size());
        for (int index = 0; index < orders.size(); index++) {
            List<Predicate> terms = new ArrayList<>(index + 1);
            for (int prefix = 0; prefix < index; prefix++) {
                OrderBy<?> order = orders.get(prefix);
                Expression<?> expression = nonNullResolver.resolve(
                        nonNullRoot,
                        order.getField()
                );
                Object prefixValue = values.get(prefix);
                terms.add(prefixValue == null
                        ? nonNullBuilder.isNull(expression)
                        : nonNullBuilder.equal(expression, prefixValue));
            }
            OrderBy<?> order = orders.get(index);
            Expression<?> expression = nonNullResolver.resolve(nonNullRoot, order.getField());
            terms.add(after(
                    nonNullBuilder,
                    expression,
                    values.get(index),
                    order
            ));
            branches.add(nonNullBuilder.and(terms.toArray(Predicate[]::new)));
        }
        return nonNullBuilder.or(branches.toArray(Predicate[]::new));
    }

    /**
     * Creates JPA ordering terms using the default direct same-name field resolver.
     *
     * @param builder  the JPA criteria builder
     * @param root     the query entity root
     * @param criteria structured conditions and ordering
     * @param <T>      the entity type
     * @return immutable JPA order terms
     */
    public <T> List<Order> orders(
            CriteriaBuilder builder,
            Root<T> root,
            QueryCriteria criteria
    ) {
        return orders(builder, root, criteria, JpaQueryFieldResolver.byAttributeName());
    }

    /**
     * Creates JPA ordering terms using a direct attribute resolver with a naming strategy.
     *
     * @param builder        the JPA criteria builder
     * @param root           the query entity root
     * @param criteria       structured conditions and ordering
     * @param namingStrategy the query-to-attribute naming strategy
     * @param <T>            the entity type
     * @return immutable JPA order terms
     */
    public <T> List<Order> orders(
            CriteriaBuilder builder,
            Root<T> root,
            QueryCriteria criteria,
            JpaFieldNamingStrategy namingStrategy
    ) {
        return orders(builder, root, criteria, JpaQueryFieldResolver.byAttributeName(namingStrategy));
    }

    /**
     * Creates JPA ordering terms for the supplied explicit typed ordering.
     *
     * <p>This overload is an advanced mapping extension point. Use the overload without a
     * resolver for direct same-name attributes.</p>
     *
     * @param builder       the JPA criteria builder
     * @param root          the query entity root
     * @param criteria      structured conditions and ordering
     * @param fieldResolver explicit allowed typed field resolver
     * @param <T>           the entity type
     * @return immutable JPA order terms
     */
    public <T> List<Order> orders(
            CriteriaBuilder builder,
            Root<T> root,
            QueryCriteria criteria,
            JpaQueryFieldResolver<T> fieldResolver
    ) {
        CriteriaBuilder nonNullBuilder = Objects.requireNonNull(builder, "builder");
        Root<T> nonNullRoot = Objects.requireNonNull(root, "root");
        QueryCriteria nonNullCriteria = Objects.requireNonNull(criteria, "criteria");
        JpaQueryFieldResolver<T> nonNullFieldResolver = Objects.requireNonNull(fieldResolver, "fieldResolver");
        List<Order> orders = new ArrayList<>();
        for (OrderBy<?> order : nonNullCriteria.getOrders()) {
            orders.add(order(nonNullBuilder, nonNullRoot, order, nonNullFieldResolver));
        }
        return List.copyOf(orders);
    }

    private <T, V> Order order(
            CriteriaBuilder builder,
            Root<T> root,
            OrderBy<V> order,
            JpaQueryFieldResolver<T> fieldResolver
    ) {
        Expression<V> expression = fieldResolver.resolve(root, order.getField());
        Nulls nulls = order.getNullPlacement() == OrderBy.NullPlacement.FIRST ? Nulls.FIRST : Nulls.LAST;
        if (order.getDirection() == OrderBy.Direction.ASCENDING) {
            return builder.asc(expression, nulls);
        }
        return builder.desc(expression, nulls);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Predicate after(
            CriteriaBuilder builder,
            Expression<?> expression,
            @Nullable Object value,
            OrderBy<?> order
    ) {
        if (value == null) {
            return order.getNullPlacement() == OrderBy.NullPlacement.FIRST
                    ? builder.isNotNull(expression)
                    : builder.disjunction();
        }
        if (!(value instanceof Comparable<?> comparable)) {
            throw new IllegalArgumentException("Keyset values must be comparable");
        }
        Expression comparableExpression = expression;
        Comparable comparableValue = comparable;
        Predicate comparison = order.getDirection() == OrderBy.Direction.ASCENDING
                ? builder.greaterThan(comparableExpression, comparableValue)
                : builder.lessThan(comparableExpression, comparableValue);
        return order.getNullPlacement() == OrderBy.NullPlacement.LAST
                ? builder.or(comparison, builder.isNull(expression))
                : comparison;
    }

    private static final class PredicateTranslator<T>
            implements QueryExpressionVisitor<Predicate>, QueryValueVisitor<Expression<?>> {
        private final CriteriaBuilder builder;
        private final From<?, ?> source;
        private final @Nullable CommonAbstractCriteria query;
        private final JpaQueryFieldResolver<T> fieldResolver;
        private final Map<QueryFunctionDefinition<?>, JpaQueryFunction<?>> functions;
        private final Map<QueryOperatorDefinition<?, ?>, JpaQueryOperator<?, ?>> operators;
        private final Map<QueryAggregateDefinition<?>, JpaQueryAggregate<?>> aggregates;

        private PredicateTranslator(
                CriteriaBuilder builder,
                From<?, ?> source,
                @Nullable CommonAbstractCriteria query,
                JpaQueryFieldResolver<T> fieldResolver,
                Map<QueryFunctionDefinition<?>, JpaQueryFunction<?>> functions,
                Map<QueryOperatorDefinition<?, ?>, JpaQueryOperator<?, ?>> operators,
                Map<QueryAggregateDefinition<?>, JpaQueryAggregate<?>> aggregates
        ) {
            this.builder = Objects.requireNonNull(builder, "builder");
            this.source = Objects.requireNonNull(source, "source");
            this.query = query;
            this.fieldResolver = Objects.requireNonNull(fieldResolver, "fieldResolver");
            this.functions = Objects.requireNonNull(functions, "functions");
            this.operators = Objects.requireNonNull(operators, "operators");
            this.aggregates = Objects.requireNonNull(aggregates, "aggregates");
        }

        private Predicate translate(QueryExpression expression) {
            QueryExpression nonNullExpression = Objects.requireNonNull(expression, "expression");
            if (!QueryExpression.isBuiltIn(nonNullExpression)) {
                throw new IllegalArgumentException("Unsupported query expression: " + nonNullExpression.getClass().getName());
            }
            return nonNullExpression.accept(this);
        }

        @Override
        public Predicate visitMatchAll() {
            return builder.conjunction();
        }

        @Override
        public <V, A> Predicate visitCondition(QueryCondition<V, A> condition) {
            Expression<V> expression = fieldResolver.resolve(source, condition.getField());
            return predicate(expression, condition.getOperator(), condition.getValues());
        }

        @Override
        public <V, A> Predicate visitValueCondition(QueryValueCondition<V, A> condition) {
            Expression<?> expression = expression(condition.getValue());
            return predicate(expression, condition.getOperator(), condition.getValues());
        }

        @Override
        public <V extends Comparable<? super V>> Predicate visitComparison(ComparisonCondition<V> condition) {
            Expression<V> expression = fieldResolver.resolve(source, condition.getField());
            return nonNullPredicate(expression, switch (condition.getOperator()) {
                case GREATER_THAN -> builder.greaterThan(expression, condition.getValue());
                case GREATER_THAN_OR_EQUAL -> builder.greaterThanOrEqualTo(expression, condition.getValue());
                case LESS_THAN -> builder.lessThan(expression, condition.getValue());
                case LESS_THAN_OR_EQUAL -> builder.lessThanOrEqualTo(expression, condition.getValue());
                default -> throw new IllegalArgumentException("Unsupported ordered query operator");
            });
        }

        @Override
        public Predicate visitLike(LikeCondition condition) {
            Expression<String> expression = fieldResolver.resolve(source, condition.getField());
            return nonNullPredicate(
                    expression,
                    builder.like(expression, condition.getPattern(), LikeCondition.ESCAPE_CHARACTER)
            );
        }

        @Override
        public Predicate visitLogical(LogicalCondition condition) {
            Predicate[] predicates = new Predicate[condition.getExpressions().size()];
            for (int index = 0; index < condition.getExpressions().size(); index++) {
                predicates[index] = translate(condition.getExpressions().get(index));
            }
            if (condition.getOperator() == LogicalOperator.AND) {
                return builder.and(predicates);
            }
            return builder.or(predicates);
        }

        @Override
        public Predicate visitNegated(NegatedQueryExpression condition) {
            return builder.not(translate(condition.getExpression()));
        }

        @Override
        public <V> Predicate visitExists(ExistsCondition<V> condition) {
            CommonAbstractCriteria nonNullQuery = query;
            if (nonNullQuery == null) {
                throw new IllegalArgumentException("EXISTS predicates require a Criteria query context");
            }
            Subquery<Integer> subquery = nonNullQuery.subquery(Integer.class);
            From<?, ?> correlated;
            if (source instanceof Root<?> root) {
                @SuppressWarnings({"rawtypes", "unchecked"})
                Root<?> correlatedRoot = subquery.correlate((Root) root);
                correlated = correlatedRoot;
            } else if (source instanceof Join<?, ?> join) {
                @SuppressWarnings({"rawtypes", "unchecked"})
                Join<?, ?> correlatedJoin = subquery.correlate((Join) join);
                correlated = correlatedJoin;
            } else {
                throw new IllegalArgumentException("Unsupported Criteria source for EXISTS predicate");
            }
            From<?, ?> related = correlated.join(condition.getRelation().getName());
            Predicate nested = new PredicateTranslator<>(
                    builder,
                    related,
                    subquery,
                    fieldResolver,
                    functions,
                    operators,
                    aggregates
            ).translate(condition.getCondition());
            subquery.select(builder.literal(1));
            subquery.where(nested);
            return builder.exists(subquery);
        }

        @Override
        public <V> Expression<V> visitField(QueryField<V> field) {
            return fieldResolver.resolve(source, field);
        }

        @Override
        public <V> Expression<V> visitLiteral(QueryValue.Literal<V> value) {
            return builder.literal(value.getValue());
        }

        @Override
        public <V> Expression<V> visitFunction(QueryValue.Function<V> value) {
            return switch (value.getFunction()) {
                case LOWER -> castExpression(builder.lower(stringExpression(expression(value.getArguments().get(0)))));
                case UPPER -> castExpression(builder.upper(stringExpression(expression(value.getArguments().get(0)))));
                case COALESCE -> builder.coalesce(
                        castExpression(expression(value.getArguments().get(0))),
                        castExpression(expression(value.getArguments().get(1)))
                );
                case CAST -> throw new IllegalArgumentException("CAST is represented by a cast value");
            };
        }

        @Override
        public <V> Expression<V> visitCustomFunction(QueryValue.CustomFunction<V> value) {
            JpaQueryFunction<?> function = functions.get(value.getDefinition());
            if (function == null) {
                throw new IllegalArgumentException("JPA query function is not registered: "
                        + value.getDefinition().getName());
            }
            List<Expression<?>> arguments = new ArrayList<>(value.getArguments().size());
            for (QueryValue<?> argument : value.getArguments()) {
                arguments.add(expression(argument));
            }
            Expression<?> translated = Objects.requireNonNull(function.translate(builder, List.copyOf(arguments)),
                    "query function expression");
            if (!value.getValueType().isAssignableFrom(translated.getJavaType())) {
                throw new IllegalStateException("JPA query function returned an incompatible expression");
            }
            return castExpression(translated);
        }

        @Override
        public <V> Expression<V> visitCast(QueryValue.Cast<V> value) {
            return expression(value.getOperand()).as(value.getTargetType());
        }

        @Override
        public <V> Expression<V> visitAggregate(QueryValue.Aggregate<V> value) {
            if (value.getFunction() instanceof QueryAggregateDefinition<?> definition) {
                return customAggregate(value, definition);
            }
            if (!(value.getFunction() instanceof QueryAggregateFunction.Standard standard)) {
                throw new IllegalArgumentException("Unsupported query aggregate");
            }
            if (standard == QueryAggregateFunction.COUNT && value.getArguments().isEmpty()) {
                return castExpression(builder.count(source));
            }
            Expression<?> expression = expression(value.getArguments().get(0));
            return aggregate(standard, expression);
        }

        private <V> Expression<V> customAggregate(
                QueryValue.Aggregate<V> value, QueryAggregateDefinition<?> definition
        ) {
            JpaQueryAggregate<?> implementation = aggregates.get(definition);
            if (implementation == null) {
                throw new IllegalArgumentException("JPA query aggregate is not registered: " + definition.getName());
            }
            List<Expression<?>> arguments = new ArrayList<>(value.getArguments().size());
            for (QueryValue<?> argument : value.getArguments()) {
                arguments.add(expression(argument));
            }
            Expression<?> translated = Objects.requireNonNull(
                    implementation.translate(builder, source, List.copyOf(arguments)), "query aggregate expression"
            );
            if (!value.getValueType().isAssignableFrom(translated.getJavaType())) {
                throw new IllegalStateException("JPA query aggregate returned an incompatible expression");
            }
            return castExpression(translated);
        }

        private Expression<?> expression(QueryValue<?> value) {
            QueryValue<?> nonNullValue = Objects.requireNonNull(value, "value");
            if (!QueryValue.isBuiltIn(nonNullValue)) {
                throw new IllegalArgumentException("Unsupported query value: " + nonNullValue.getClass().getName());
            }
            return nonNullValue.accept(this);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private <V> Expression<V> aggregate(
                QueryAggregateFunction.Standard function,
                Expression<?> expression
        ) {
            return switch (function) {
                case COUNT -> castExpression(builder.count(expression));
                case COUNT_DISTINCT -> castExpression(builder.countDistinct(expression));
                case SUM -> castExpression(builder.sum(expression.as(BigDecimal.class)));
                case AVERAGE -> castExpression(builder.avg((Expression) expression));
                case MIN -> castExpression(builder.least((Expression) expression));
                case MAX -> castExpression(builder.greatest((Expression) expression));
            };
        }

        private Predicate predicate(
                Expression<?> expression,
                QueryOperator operator,
                List<?> values
        ) {
            if (operator instanceof QueryOperatorDefinition<?, ?> definition) {
                return customPredicate(definition, expression, values);
            }
            if (!(operator instanceof QueryOperator.Standard standard)) {
                throw new IllegalArgumentException("Unsupported query operator");
            }
            if (operator == QueryOperator.IS_NULL) {
                return builder.isNull(expression);
            }
            if (operator == QueryOperator.IS_NOT_NULL) {
                return builder.isNotNull(expression);
            }
            Predicate valuePredicate = switch (standard) {
                case EQUAL -> builder.equal(expression, values.get(0));
                case NOT_EQUAL -> builder.notEqual(expression, values.get(0));
                case IN -> expression.in(values);
                case NOT_IN -> builder.not(expression.in(values));
                case LIKE -> builder.like(
                        stringExpression(expression),
                        (String) values.get(0),
                        LikeCondition.ESCAPE_CHARACTER
                );
                case GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL -> orderedPredicate(builder, expression, standard, values.get(0));
                case IS_NULL, IS_NOT_NULL -> throw new IllegalStateException("Null operators were handled above");
            };
            return nonNullPredicate(expression, valuePredicate);
        }

        @SuppressWarnings("unchecked")
        private <V, A> Predicate customPredicate(
                QueryOperatorDefinition<V, A> definition, Expression<?> expression, List<?> values
        ) {
            JpaQueryOperator<?, ?> implementation = operators.get(definition);
            if (implementation == null) {
                throw new IllegalArgumentException("JPA query operator is not registered: " + definition.getName());
            }
            List<A> arguments = values.stream().map(definition.getArgumentType()::cast).toList();
            return Objects.requireNonNull(((JpaQueryOperator<V, A>) implementation)
                    .translate(builder, castExpression(expression), arguments), "query operator predicate");
        }

        private Predicate nonNullPredicate(Expression<?> expression, Predicate predicate) {
            return builder.and(builder.isNotNull(expression), predicate);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static Predicate orderedPredicate(
                CriteriaBuilder builder,
                Expression<?> expression,
                QueryOperator.Standard operator,
                Object value
        ) {
            Expression comparableExpression = expression;
            Comparable comparableValue = (Comparable) value;
            return switch (operator) {
                case GREATER_THAN -> builder.greaterThan(comparableExpression, comparableValue);
                case GREATER_THAN_OR_EQUAL -> builder.greaterThanOrEqualTo(comparableExpression, comparableValue);
                case LESS_THAN -> builder.lessThan(comparableExpression, comparableValue);
                case LESS_THAN_OR_EQUAL -> builder.lessThanOrEqualTo(comparableExpression, comparableValue);
                default -> throw new IllegalArgumentException("Unsupported ordered query operator");
            };
        }

        @SuppressWarnings("unchecked")
        private static <V> Expression<V> castExpression(Expression<?> expression) {
            return (Expression<V>) expression;
        }

        private static Expression<String> stringExpression(Expression<?> expression) {
            return castExpression(expression);
        }
    }
}
