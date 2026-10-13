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


import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Declares fields, operators, and ordering permitted by one query role.
 *
 * @author RollW
 */
public final class QuerySchema {
    private final Map<String, FieldDefinition> fields;
    private final Map<QueryRelation<?>, QuerySchema> relations;

    private QuerySchema(
            Map<String, FieldDefinition> fields,
            Map<QueryRelation<?>, QuerySchema> relations
    ) {
        this.fields = Map.copyOf(fields);
        this.relations = Map.copyOf(relations);
    }

    /**
     * Starts a schema definition.
     *
     * @return the schema builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Validates fields, operators, ordering, and structural complexity for one query.
     *
     * @param criteria the query criteria to validate
     * @param limits   the role-specific complexity limits
     */
    public void validate(QueryCriteria criteria, QueryComplexityBudget limits) {
        QueryCriteria nonNullCriteria = Objects.requireNonNull(criteria, "criteria");
        QueryComplexityBudget nonNullLimits = Objects.requireNonNull(limits, "limits");
        validateOrders(nonNullCriteria.getOrders());
        validateExpression(nonNullCriteria.getCondition(), nonNullLimits, 1, new ValidationState());
    }

    /**
     * Validates the source criteria, selected values, grouping, and HAVING condition of one plan.
     *
     * @param plan   the query plan to validate
     * @param limits the role-specific complexity limits
     */
    public void validate(QueryPlan<?> plan, QueryComplexityBudget limits) {
        QueryPlan<?> nonNullPlan = Objects.requireNonNull(plan, "plan");
        QueryComplexityBudget nonNullLimits = Objects.requireNonNull(limits, "limits");
        validate(nonNullPlan.getCriteria(), nonNullLimits);
        if (nonNullPlan instanceof TimeSeriesQueryPlan<?> timeSeries) {
            validateValue(timeSeries.getTimestamp());
            if (timeSeries.getValue() != null) {
                validateValue(timeSeries.getValue());
            }
            return;
        }
        if (!(nonNullPlan instanceof ProjectionQueryPlan<?> projectionPlan)) {
            throw new IllegalArgumentException("Unsupported query plan: " + nonNullPlan.getClass().getName());
        }
        QueryProjection<?> projection = projectionPlan.getProjection();
        for (QueryValue<?> value : projection.getValues()) {
            validateValue(value);
        }
        for (QueryValue<?> value : projectionPlan.getGroupBy()) {
            if (QueryValue.containsAggregate(value)) {
                throw new IllegalArgumentException("GROUP BY values must not contain aggregates");
            }
            validateValue(value);
        }
        if (projectionPlan.getHaving() != null) {
            validateExpression(projectionPlan.getHaving(), nonNullLimits, 1, new ValidationState());
        }
    }

    private void validateOrders(List<OrderBy<?>> orders) {
        for (OrderBy<?> order : orders) {
            FieldDefinition definition = findDefinition(order.getField());
            if (!definition.orderable) {
                throw new IllegalArgumentException("Query field is not orderable: " + order.getField().getName());
            }
        }
    }

    private void validateExpression(
            QueryExpression expression,
            QueryComplexityBudget limits,
            int depth,
            ValidationState state
    ) {
        if (expression instanceof MatchAllQueryExpression) {
            return;
        }
        if (depth > limits.getMaximumDepth()) {
            throw new IllegalArgumentException("Query expression exceeds the maximum depth");
        }
        state.recordNode(limits);
        if (expression instanceof QueryCondition<?> condition) {
            validateCondition(condition);
            if (condition.getValues().size() > limits.getMaximumMembershipValues()
                    && (condition.getOperator() == QueryOperator.IN || condition.getOperator() == QueryOperator.NOT_IN)) {
                throw new IllegalArgumentException("Membership condition exceeds the maximum value count");
            }
            return;
        }
        if (expression instanceof QueryValueCondition<?> condition) {
            validateValueCondition(condition, limits);
            return;
        }
        if (expression instanceof ComparisonCondition<?> condition) {
            validateOperator(condition.getField(), condition.getOperator());
            return;
        }
        if (expression instanceof LikeCondition condition) {
            validateOperator(condition.getField(), QueryOperator.LIKE);
            if (condition.getPattern().length() > limits.getMaximumLikePatternLength()) {
                throw new IllegalArgumentException("LIKE pattern exceeds the maximum length");
            }
            return;
        }
        if (expression instanceof LogicalCondition condition) {
            if (condition.getOperator() == LogicalOperator.OR
                    && condition.getExpressions().size() > limits.getMaximumOrBranches()) {
                throw new IllegalArgumentException("OR condition exceeds the maximum branch count");
            }
            for (QueryExpression child : condition.getExpressions()) {
                validateExpression(child, limits, depth + 1, state);
            }
            return;
        }
        if (expression instanceof NegatedQueryExpression condition) {
            validateExpression(condition.getExpression(), limits, depth + 1, state);
            return;
        }
        if (expression instanceof ExistsCondition<?> condition) {
            QuerySchema relatedSchema = relations.get(condition.getRelation());
            if (relatedSchema == null) {
                throw new IllegalArgumentException(
                        "Query relation is not part of this schema: " + condition.getRelation().getName()
                );
            }
            relatedSchema.validateExpression(condition.getCondition(), limits, depth + 1, state);
            return;
        }
        throw new IllegalArgumentException("Unsupported query expression");
    }

    private void validateCondition(QueryCondition<?> condition) {
        validateOperator(condition.getField(), condition.getOperator());
    }

    private void validateValueCondition(QueryValueCondition<?> condition, QueryComplexityBudget limits) {
        validateValue(condition.getValue());
        for (QueryField<?> field : referencedFields(condition.getValue())) {
            validateOperator(field, condition.getOperator());
        }
        if (condition.getValues().size() > limits.getMaximumMembershipValues()
                && (condition.getOperator() == QueryOperator.IN || condition.getOperator() == QueryOperator.NOT_IN)) {
            throw new IllegalArgumentException("Membership condition exceeds the maximum value count");
        }
        if (condition.getOperator() == QueryOperator.LIKE
                && condition.getValues().get(0).toString().length() > limits.getMaximumLikePatternLength()) {
            throw new IllegalArgumentException("LIKE pattern exceeds the maximum length");
        }
    }

    private void validateValue(QueryValue<?> value) {
        if (value instanceof QueryValue.Field<?> field) {
            findDefinition(field.getField());
            return;
        }
        if (value instanceof QueryValue.Literal<?>) {
            return;
        }
        if (value instanceof QueryValue.Function<?> function) {
            for (QueryValue<?> argument : function.getArguments()) {
                validateValue(argument);
            }
            for (QueryField<?> field : referencedFields(function)) {
                validateFunction(field, function.getFunction());
            }
            return;
        }
        if (value instanceof QueryValue.Cast<?> cast) {
            validateValue(cast.getOperand());
            for (QueryField<?> field : referencedFields(cast.getOperand())) {
                validateFunction(field, QueryFunction.CAST);
            }
            return;
        }
        if (value instanceof QueryValue.Aggregate<?> aggregate) {
            if (aggregate.getOperand() == null) {
                return;
            }
            validateValue(aggregate.getOperand());
            for (QueryField<?> field : referencedFields(aggregate.getOperand())) {
                validateAggregate(field, aggregate.getFunction());
            }
            return;
        }
        throw new IllegalArgumentException("Unsupported query value");
    }

    private Set<QueryField<?>> referencedFields(QueryValue<?> value) {
        Set<QueryField<?>> fields = new LinkedHashSet<>();
        collectFields(value, fields);
        return fields;
    }

    private void collectFields(QueryValue<?> value, Set<QueryField<?>> fields) {
        if (value instanceof QueryValue.Field<?> field) {
            fields.add(field.getField());
            return;
        }
        if (value instanceof QueryValue.Function<?> function) {
            for (QueryValue<?> argument : function.getArguments()) {
                collectFields(argument, fields);
            }
            return;
        }
        if (value instanceof QueryValue.Cast<?> cast) {
            collectFields(cast.getOperand(), fields);
            return;
        }
        if (value instanceof QueryValue.Aggregate<?> aggregate && aggregate.getOperand() != null) {
            collectFields(aggregate.getOperand(), fields);
        }
    }

    private void validateFunction(QueryField<?> field, QueryFunction function) {
        FieldDefinition definition = findDefinition(field);
        if (!definition.functions.contains(function)) {
            throw new IllegalArgumentException("Query function is not allowed for field: " + field.getName());
        }
    }

    private void validateOperator(QueryField<?> field, QueryOperator operator) {
        FieldDefinition definition = findDefinition(field);
        if (!definition.operators.contains(operator)) {
            throw new IllegalArgumentException("Query operator is not allowed for field: " + field.getName());
        }
    }

    private void validateAggregate(QueryField<?> field, QueryAggregateFunction aggregate) {
        FieldDefinition definition = findDefinition(field);
        if (!definition.aggregates.contains(aggregate)) {
            throw new IllegalArgumentException("Query aggregate is not allowed for field: " + field.getName());
        }
    }

    private FieldDefinition findDefinition(QueryField<?> field) {
        FieldDefinition definition = fields.get(field.getName());
        if (definition == null) {
            throw new IllegalArgumentException("Query field is not part of this schema: " + field.getName());
        }
        if (!definition.field.equals(field)) {
            throw new IllegalArgumentException("Query field type does not match this schema: " + field.getName());
        }
        return definition;
    }

    /**
     * Builds one immutable query schema.
     */
    public static final class Builder {
        private final Map<String, FieldDefinition> fields = new LinkedHashMap<>();
        private final Map<QueryRelation<?>, QuerySchema> relations = new LinkedHashMap<>();

        private Builder() {
        }

        /**
         * Adds a filterable but non-orderable field.
         *
         * @param field     the typed domain field
         * @param operators the supported field operators
         * @param <T>       the field value type
         * @return this builder
         */
        public <T> Builder add(QueryField<T> field, Set<QueryOperator> operators) {
            return add(field, operators, false);
        }

        /**
         * Adds a field with its supported operations and ordering capability.
         *
         * @param field     the typed domain field
         * @param operators the supported field operators, which may be empty for an ordering-only field
         * @param orderable whether the field may be used for ordering
         * @param <T>       the field value type
         * @return this builder
         */
        public <T> Builder add(QueryField<T> field, Set<QueryOperator> operators, boolean orderable) {
            return add(field, operators, orderable, Set.of());
        }

        /**
         * Adds a field with its supported operations, ordering capability, and value functions.
         *
         * @param field     the typed domain field
         * @param operators the supported field operators
         * @param orderable whether the field may be used for ordering
         * @param functions the supported value functions and casts
         * @param <T>       the field value type
         * @return this builder
         */
        public <T> Builder add(
                QueryField<T> field,
                Set<QueryOperator> operators,
                boolean orderable,
                Set<QueryFunction> functions
        ) {
            return add(field, operators, orderable, functions, Set.of());
        }

        /**
         * Adds a field with its supported operations, value functions, and aggregate functions.
         *
         * @param field      the typed domain field
         * @param operators  the supported field operators
         * @param orderable  whether the field may be used for ordering
         * @param functions  the supported value functions and casts
         * @param aggregates the supported aggregate functions
         * @param <T>        the field value type
         * @return this builder
         */
        public <T> Builder add(
                QueryField<T> field,
                Set<QueryOperator> operators,
                boolean orderable,
                Set<QueryFunction> functions,
                Set<QueryAggregateFunction> aggregates
        ) {
            QueryField<T> nonNullField = Objects.requireNonNull(field, "field");
            Set<QueryOperator> copiedOperators = Set.copyOf(Objects.requireNonNull(operators, "operators"));
            Set<QueryFunction> copiedFunctions = Set.copyOf(Objects.requireNonNull(functions, "functions"));
            Set<QueryAggregateFunction> copiedAggregates = Set.copyOf(
                    Objects.requireNonNull(aggregates, "aggregates")
            );
            if (copiedOperators.isEmpty() && !orderable) {
                throw new IllegalArgumentException("Query fields must support filtering or ordering");
            }
            validateOperatorTypes(nonNullField, copiedOperators, copiedFunctions);
            FieldDefinition definition = new FieldDefinition(
                    nonNullField,
                    copiedOperators,
                    copiedFunctions,
                    copiedAggregates,
                    orderable
            );
            FieldDefinition previous = fields.putIfAbsent(nonNullField.getName(), definition);
            if (previous != null) {
                throw new IllegalArgumentException("Query schema contains a duplicate field: " + nonNullField.getName());
            }
            return this;
        }

        /**
         * Registers the schema used by an allowed related collection.
         *
         * @param relation      the allowed to-many relationship
         * @param relatedSchema the schema for related candidates
         * @param <T>           the related candidate type
         * @return this builder
         */
        public <T> Builder relation(QueryRelation<T> relation, QuerySchema relatedSchema) {
            QueryRelation<T> nonNullRelation = Objects.requireNonNull(relation, "relation");
            if (nonNullRelation.getCardinality() != QueryRelation.Cardinality.TO_MANY) {
                throw new IllegalArgumentException("EXISTS schemas require a to-many relation");
            }
            QuerySchema nonNullSchema = Objects.requireNonNull(relatedSchema, "relatedSchema");
            QuerySchema previous = relations.putIfAbsent(nonNullRelation, nonNullSchema);
            if (previous != null && previous != nonNullSchema) {
                throw new IllegalArgumentException(
                        "Query schema contains a duplicate relation: " + nonNullRelation.getName()
                );
            }
            return this;
        }

        /**
         * Creates the immutable schema.
         *
         * @return the query schema
         */
        public QuerySchema build() {
            return new QuerySchema(fields, relations);
        }

        private static void validateOperatorTypes(
                QueryField<?> field,
                Set<QueryOperator> operators,
                Set<QueryFunction> functions
        ) {
            for (QueryOperator operator : operators) {
                if (operator == QueryOperator.LIKE
                        && field.getValueType() != String.class
                        && !functions.contains(QueryFunction.CAST)) {
                    throw new IllegalArgumentException("LIKE is only supported for String fields");
                }
                if (isOrderedComparison(operator) && !Comparable.class.isAssignableFrom(field.getValueType())) {
                    throw new IllegalArgumentException("Ordered comparisons require a Comparable field type");
                }
            }
        }

        private static boolean isOrderedComparison(QueryOperator operator) {
            return operator == QueryOperator.GREATER_THAN
                    || operator == QueryOperator.GREATER_THAN_OR_EQUAL
                    || operator == QueryOperator.LESS_THAN
                    || operator == QueryOperator.LESS_THAN_OR_EQUAL;
        }
    }

    private static final class FieldDefinition {
        private final QueryField<?> field;
        private final Set<QueryOperator> operators;
        private final Set<QueryFunction> functions;
        private final Set<QueryAggregateFunction> aggregates;
        private final boolean orderable;

        private FieldDefinition(
                QueryField<?> field,
                Set<QueryOperator> operators,
                Set<QueryFunction> functions,
                Set<QueryAggregateFunction> aggregates,
                boolean orderable
        ) {
            this.field = field;
            this.operators = operators;
            this.functions = functions;
            this.aggregates = aggregates;
            this.orderable = orderable;
        }
    }

    private static final class ValidationState {
        private int nodeCount;

        private void recordNode(QueryComplexityBudget limits) {
            nodeCount++;
            if (nodeCount > limits.getMaximumNodes()) {
                throw new IllegalArgumentException("Query expression exceeds the maximum node count");
            }
        }
    }
}
