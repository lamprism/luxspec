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
import com.lamprism.luxspec.data.query.QueryAggregateFunction;
import com.lamprism.luxspec.data.query.QueryCondition;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.data.query.QueryExpression;
import com.lamprism.luxspec.data.query.QueryExpressionVisitor;
import com.lamprism.luxspec.data.query.QueryField;
import com.lamprism.luxspec.data.query.QueryOperator;
import com.lamprism.luxspec.data.query.QueryValue;
import com.lamprism.luxspec.data.query.QueryValueCondition;
import com.lamprism.luxspec.data.query.QueryValueVisitor;
import jakarta.persistence.criteria.CommonAbstractCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Translates the typed Luxspec structured-query model into JPA Criteria API objects.
 *
 * @author RollW
 */
public class JpaCriteriaTranslator {
    /**
     * Creates a stateless typed Criteria API translator.
     */
    public JpaCriteriaTranslator() {
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
        return new PredicateTranslator<>(builder, root, query, fieldResolver).translate(expression);
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
        return new PredicateTranslator<>(builder, root, query, fieldResolver).expression(value);
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
        List<Object> values = nonNullPosition.values();
        if (orders.isEmpty()) {
            throw new IllegalArgumentException("Keyset windows require explicit query ordering");
        }
        if (values.size() != orders.size()) {
            throw new IllegalArgumentException("Keyset position does not match query ordering");
        }
        for (int index = 0; index < orders.size(); index++) {
            Object value = values.get(index);
            if (!orders.get(index).getField().getValueType().isInstance(value)) {
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
                terms.add(nonNullBuilder.equal(expression, values.get(prefix)));
            }
            OrderBy<?> order = orders.get(index);
            Expression<?> expression = nonNullResolver.resolve(nonNullRoot, order.getField());
            terms.add(after(
                    nonNullBuilder,
                    expression,
                    values.get(index),
                    order.getDirection()
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
        if (order.getDirection() == OrderBy.Direction.ASCENDING) {
            return builder.asc(expression);
        }
        return builder.desc(expression);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Predicate after(
            CriteriaBuilder builder,
            Expression<?> expression,
            Object value,
            OrderBy.Direction direction
    ) {
        if (!(value instanceof Comparable<?> comparable)) {
            throw new IllegalArgumentException("Keyset values must be comparable");
        }
        Expression comparableExpression = expression;
        Comparable comparableValue = comparable;
        if (direction == OrderBy.Direction.ASCENDING) {
            return builder.greaterThan(comparableExpression, comparableValue);
        }
        return builder.lessThan(comparableExpression, comparableValue);
    }

    private static final class PredicateTranslator<T>
            implements QueryExpressionVisitor<Predicate>, QueryValueVisitor<Expression<?>> {
        private final CriteriaBuilder builder;
        private final From<?, ?> source;
        private final @Nullable CommonAbstractCriteria query;
        private final JpaQueryFieldResolver<T> fieldResolver;

        private PredicateTranslator(
                CriteriaBuilder builder,
                From<?, ?> source,
                @Nullable CommonAbstractCriteria query,
                JpaQueryFieldResolver<T> fieldResolver
        ) {
            this.builder = Objects.requireNonNull(builder, "builder");
            this.source = Objects.requireNonNull(source, "source");
            this.query = query;
            this.fieldResolver = Objects.requireNonNull(fieldResolver, "fieldResolver");
        }

        private Predicate translate(QueryExpression expression) {
            return Objects.requireNonNull(expression, "expression").accept(this);
        }

        @Override
        public Predicate visitMatchAll() {
            return builder.conjunction();
        }

        @Override
        public <V> Predicate visitCondition(QueryCondition<V> condition) {
            Expression<V> expression = fieldResolver.resolve(source, condition.getField());
            return predicate(expression, condition.getOperator(), condition.getValues());
        }

        @Override
        public <V> Predicate visitValueCondition(QueryValueCondition<V> condition) {
            Expression<?> expression = expression(condition.getValue());
            return predicate(expression, condition.getOperator(), condition.getValues());
        }

        @Override
        public <V extends Comparable<? super V>> Predicate visitComparison(ComparisonCondition<V> condition) {
            Expression<V> expression = fieldResolver.resolve(source, condition.getField());
            return switch (condition.getOperator()) {
                case GREATER_THAN -> builder.greaterThan(expression, condition.getValue());
                case GREATER_THAN_OR_EQUAL -> builder.greaterThanOrEqualTo(expression, condition.getValue());
                case LESS_THAN -> builder.lessThan(expression, condition.getValue());
                case LESS_THAN_OR_EQUAL -> builder.lessThanOrEqualTo(expression, condition.getValue());
                default -> throw new IllegalArgumentException("Unsupported ordered query operator");
            };
        }

        @Override
        public Predicate visitLike(LikeCondition condition) {
            Expression<String> expression = fieldResolver.resolve(source, condition.getField());
            return builder.like(expression, condition.getPattern(), LikeCondition.ESCAPE_CHARACTER);
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
                    fieldResolver
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
        public <V> Expression<V> visitCast(QueryValue.Cast<V> value) {
            return expression(value.getOperand()).as(value.getTargetType());
        }

        @Override
        public <V> Expression<V> visitAggregate(QueryValue.Aggregate<V> value) {
            QueryValue<?> operand = value.getOperand();
            if (value.getFunction() == QueryAggregateFunction.COUNT && operand == null) {
                return castExpression(builder.count(source));
            }
            Expression<?> expression = expression(Objects.requireNonNull(operand, "aggregate operand"));
            return aggregate(value.getFunction(), expression);
        }

        private Expression<?> expression(QueryValue<?> value) {
            return Objects.requireNonNull(value, "value").accept(this);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private <V> Expression<V> aggregate(
                QueryAggregateFunction function,
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
            return switch (operator) {
                case EQUAL -> builder.equal(expression, values.get(0));
                case NOT_EQUAL -> builder.notEqual(expression, values.get(0));
                case IN -> expression.in(values);
                case NOT_IN -> builder.not(expression.in(values));
                case IS_NULL -> builder.isNull(expression);
                case IS_NOT_NULL -> builder.isNotNull(expression);
                case LIKE -> builder.like(
                        stringExpression(expression),
                        (String) values.get(0),
                        LikeCondition.ESCAPE_CHARACTER
                );
                case GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL ->
                        orderedPredicate(builder, expression, operator, values.get(0));
            };
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static Predicate orderedPredicate(
                CriteriaBuilder builder,
                Expression<?> expression,
                QueryOperator operator,
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
