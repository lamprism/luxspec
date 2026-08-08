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

import com.lamprism.luxspec.data.query.ComparisonCondition;
import com.lamprism.luxspec.data.query.LikeCondition;
import com.lamprism.luxspec.data.query.LogicalCondition;
import com.lamprism.luxspec.data.query.LogicalOperator;
import com.lamprism.luxspec.data.query.NegatedQueryExpression;
import com.lamprism.luxspec.data.query.OrderBy;
import com.lamprism.luxspec.data.query.QueryCondition;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.data.query.QueryExpression;
import com.lamprism.luxspec.data.query.QueryExpressionVisitor;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Translates the typed Luxspec structured-query model into JPA Criteria API objects.
 *
 * @author RollW
 */
public final class JpaCriteriaTranslator {
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
        return predicate(builder, root, criteria, JpaFieldResolver.byAttributeName());
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
        return predicate(builder, root, criteria, JpaFieldResolver.byAttributeName(namingStrategy));
    }

    /**
     * Creates a predicate for the complete structured filter expression.
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
            JpaFieldResolver<T> fieldResolver
    ) {
        return new PredicateTranslator<>(builder, root, criteria, fieldResolver).translate();
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
        return orders(builder, root, criteria, JpaFieldResolver.byAttributeName());
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
        return orders(builder, root, criteria, JpaFieldResolver.byAttributeName(namingStrategy));
    }

    /**
     * Creates JPA ordering terms for the supplied explicit typed ordering.
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
            JpaFieldResolver<T> fieldResolver
    ) {
        CriteriaBuilder nonNullBuilder = Objects.requireNonNull(builder, "builder");
        Root<T> nonNullRoot = Objects.requireNonNull(root, "root");
        QueryCriteria nonNullCriteria = Objects.requireNonNull(criteria, "criteria");
        JpaFieldResolver<T> nonNullFieldResolver = Objects.requireNonNull(fieldResolver, "fieldResolver");
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
            JpaFieldResolver<T> fieldResolver
    ) {
        Expression<V> expression = fieldResolver.resolve(root, order.getField());
        if (order.getDirection() == OrderBy.Direction.ASCENDING) {
            return builder.asc(expression);
        }
        return builder.desc(expression);
    }

    private static final class PredicateTranslator<T> implements QueryExpressionVisitor<Predicate> {
        private final CriteriaBuilder builder;
        private final Root<T> root;
        private final QueryCriteria criteria;
        private final JpaFieldResolver<T> fieldResolver;

        private PredicateTranslator(
                CriteriaBuilder builder,
                Root<T> root,
                QueryCriteria criteria,
                JpaFieldResolver<T> fieldResolver
        ) {
            this.builder = Objects.requireNonNull(builder, "builder");
            this.root = Objects.requireNonNull(root, "root");
            this.criteria = Objects.requireNonNull(criteria, "criteria");
            this.fieldResolver = Objects.requireNonNull(fieldResolver, "fieldResolver");
        }

        private Predicate translate() {
            return translate(criteria.getCondition());
        }

        @Override
        public Predicate visitMatchAll() {
            return builder.conjunction();
        }

        @Override
        public <V> Predicate visitCondition(QueryCondition<V> condition) {
            Expression<V> expression = fieldResolver.resolve(root, condition.getField());
            return switch (condition.getOperator()) {
                case EQUAL -> builder.equal(expression, condition.getValues().get(0));
                case NOT_EQUAL -> builder.notEqual(expression, condition.getValues().get(0));
                case IN -> expression.in(condition.getValues());
                case NOT_IN -> builder.not(expression.in(condition.getValues()));
                default -> throw new IllegalArgumentException("Unsupported simple query operator");
            };
        }

        @Override
        public <V extends Comparable<? super V>> Predicate visitComparison(ComparisonCondition<V> condition) {
            Expression<V> expression = fieldResolver.resolve(root, condition.getField());
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
            Expression<String> expression = fieldResolver.resolve(root, condition.getField());
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

        private Predicate translate(QueryExpression expression) {
            return Objects.requireNonNull(expression, "expression").accept(this);
        }
    }
}
