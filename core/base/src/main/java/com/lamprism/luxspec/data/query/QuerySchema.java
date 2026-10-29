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
    private final Map<QueryFunctionDefinition<?>, Set<QueryField<?>>> customFunctions;
    private final Set<QueryOperatorDefinition<?, ?>> fieldFreeOperators;
    private final Set<QueryAggregateDefinition<?>> fieldFreeAggregates;
    private final Map<TimeSeriesAggregationFunction, Set<QueryField<?>>> customTimeSeriesAggregations;

    private QuerySchema(
            Map<String, FieldDefinition> fields,
            Map<QueryRelation<?>, QuerySchema> relations,
            Map<QueryFunctionDefinition<?>, Set<QueryField<?>>> customFunctions,
            Set<QueryOperatorDefinition<?, ?>> fieldFreeOperators,
            Set<QueryAggregateDefinition<?>> fieldFreeAggregates,
            Map<TimeSeriesAggregationFunction, Set<QueryField<?>>> customTimeSeriesAggregations
    ) {
        this.fields = Map.copyOf(fields);
        this.relations = Map.copyOf(relations);
        this.customFunctions = Map.copyOf(customFunctions);
        this.fieldFreeOperators = Set.copyOf(fieldFreeOperators);
        this.fieldFreeAggregates = Set.copyOf(fieldFreeAggregates);
        this.customTimeSeriesAggregations = Map.copyOf(customTimeSeriesAggregations);
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
        validateExpression(nonNullCriteria.getCondition(), nonNullLimits, 1, new ValidationState(), false);
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
        ValidationState state = new ValidationState();
        validateOrders(nonNullPlan.getCriteria().getOrders());
        validateExpression(nonNullPlan.getCriteria().getCondition(), nonNullLimits, 1, state, false);
        if (nonNullPlan instanceof TimeSeriesQueryPlan<?> timeSeries) {
            validateValue(timeSeries.getTimestamp(), nonNullLimits, 1, state);
            if (timeSeries.getValue() != null) {
                validateValue(timeSeries.getValue(), nonNullLimits, 1, state);
            }
            validateTimeSeriesAggregation(timeSeries);
            return;
        }
        if (!(nonNullPlan instanceof ProjectionQueryPlan<?> projectionPlan)) {
            throw new IllegalArgumentException("Unsupported query plan: " + nonNullPlan.getClass().getName());
        }
        QueryProjection<?> projection = projectionPlan.getProjection();
        for (QueryValue<?> value : projection.getValues()) {
            validateValue(value, nonNullLimits, 1, state);
        }
        for (QueryValue<?> value : projectionPlan.getGroupBy()) {
            if (QueryValue.containsAggregate(value)) {
                throw new IllegalArgumentException("GROUP BY values must not contain aggregates");
            }
            validateValue(value, nonNullLimits, 1, state);
        }
        if (projectionPlan.getHaving() != null) {
            validateExpression(projectionPlan.getHaving(), nonNullLimits, 1, state, true);
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
            ValidationState state,
            boolean allowAggregate
    ) {
        if (depth > limits.getMaximumDepth()) {
            throw new IllegalArgumentException("Query expression exceeds the maximum depth");
        }
        state.recordNode(limits);
        if (expression instanceof MatchAllQueryExpression) {
            return;
        }
        if (expression instanceof QueryCondition<?, ?> condition) {
            validateCondition(condition);
            if (condition.getValues().size() > limits.getMaximumMembershipValues()
                    && (condition.getOperator() == QueryOperator.IN || condition.getOperator() == QueryOperator.NOT_IN
                    || condition.getOperator() instanceof QueryOperatorDefinition<?, ?>)) {
                throw new IllegalArgumentException("Membership condition exceeds the maximum value count");
            }
            return;
        }
        if (expression instanceof QueryValueCondition<?, ?> condition) {
            if (!allowAggregate && QueryValue.containsAggregate(condition.getValue())) {
                throw new IllegalArgumentException("Aggregate values are not allowed in source conditions");
            }
            validateValueCondition(condition, limits, depth, state);
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
                validateExpression(child, limits, depth + 1, state, allowAggregate);
            }
            return;
        }
        if (expression instanceof NegatedQueryExpression condition) {
            validateExpression(condition.getExpression(), limits, depth + 1, state, allowAggregate);
            return;
        }
        if (expression instanceof ExistsCondition<?> condition) {
            QuerySchema relatedSchema = relations.get(condition.getRelation());
            if (relatedSchema == null) {
                throw new IllegalArgumentException(
                        "Query relation is not part of this schema: " + condition.getRelation().getName()
                );
            }
            relatedSchema.validateExpression(condition.getCondition(), limits, depth + 1, state, false);
            return;
        }
        throw new IllegalArgumentException("Unsupported query expression");
    }

    private void validateCondition(QueryCondition<?, ?> condition) {
        validateOperator(condition.getField(), condition.getOperator());
    }

    private void validateValueCondition(
            QueryValueCondition<?, ?> condition, QueryComplexityBudget limits, int depth, ValidationState state
    ) {
        validateValue(condition.getValue(), limits, depth + 1, state);
        Set<QueryField<?>> referenced = referencedFields(condition.getValue());
        if (referenced.isEmpty() && condition.getOperator() instanceof QueryOperatorDefinition<?, ?> operator
                && !fieldFreeOperators.contains(operator)) {
            throw new IllegalArgumentException("Field-free query operator is not allowed: " + operator.getName());
        }
        for (QueryField<?> field : referenced) {
            validateOperator(field, condition.getOperator());
        }
        if (condition.getValues().size() > limits.getMaximumMembershipValues()
                && (condition.getOperator() == QueryOperator.IN || condition.getOperator() == QueryOperator.NOT_IN
                || condition.getOperator() instanceof QueryOperatorDefinition<?, ?>)) {
            throw new IllegalArgumentException("Membership condition exceeds the maximum value count");
        }
        if (condition.getOperator() == QueryOperator.LIKE
                && condition.getValues().get(0).toString().length() > limits.getMaximumLikePatternLength()) {
            throw new IllegalArgumentException("LIKE pattern exceeds the maximum length");
        }
    }

    private void validateValue(QueryValue<?> value, QueryComplexityBudget limits, int depth, ValidationState state) {
        if (depth > limits.getMaximumDepth()) {
            throw new IllegalArgumentException("Query value exceeds the maximum depth");
        }
        state.recordNode(limits);
        if (value instanceof QueryValue.Field<?> field) {
            findDefinition(field.getField());
            return;
        }
        if (value instanceof QueryValue.Literal<?>) {
            return;
        }
        if (value instanceof QueryValue.Function<?> function) {
            for (QueryValue<?> argument : function.getArguments()) {
                validateValue(argument, limits, depth + 1, state);
            }
            for (QueryField<?> field : referencedFields(function)) {
                validateFunction(field, function.getFunction());
            }
            return;
        }
        if (value instanceof QueryValue.CustomFunction<?> function) {
            Set<QueryField<?>> allowedFields = customFunctions.get(function.getDefinition());
            if (allowedFields == null) {
                throw new IllegalArgumentException("Query function is not allowed: " + function.getDefinition().getName());
            }
            for (QueryValue<?> argument : function.getArguments()) {
                validateValue(argument, limits, depth + 1, state);
            }
            for (QueryField<?> field : referencedFields(function)) {
                if (!allowedFields.contains(field)) {
                    throw new IllegalArgumentException("Query function is not allowed for field: " + field.getName());
                }
            }
            return;
        }
        if (value instanceof QueryValue.Cast<?> cast) {
            validateValue(cast.getOperand(), limits, depth + 1, state);
            for (QueryField<?> field : referencedFields(cast.getOperand())) {
                validateFunction(field, QueryFunction.CAST);
            }
            return;
        }
        if (value instanceof QueryValue.Aggregate<?> aggregate) {
            for (QueryValue<?> argument : aggregate.getArguments()) {
                validateValue(argument, limits, depth + 1, state);
            }
            Set<QueryField<?>> referenced = referencedFields(aggregate);
            if (referenced.isEmpty() && aggregate.getFunction() instanceof QueryAggregateDefinition<?> definition
                    && !fieldFreeAggregates.contains(definition)) {
                throw new IllegalArgumentException("Field-free query aggregate is not allowed: " + definition.getName());
            }
            for (QueryField<?> field : referenced) {
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
        if (value instanceof QueryValue.CustomFunction<?> function) {
            for (QueryValue<?> argument : function.getArguments()) {
                collectFields(argument, fields);
            }
            return;
        }
        if (value instanceof QueryValue.Cast<?> cast) {
            collectFields(cast.getOperand(), fields);
            return;
        }
        if (value instanceof QueryValue.Aggregate<?> aggregate) {
            for (QueryValue<?> argument : aggregate.getArguments()) {
                collectFields(argument, fields);
            }
        }
    }

    private void validateTimeSeriesAggregation(TimeSeriesQueryPlan<?> plan) {
        Set<QueryField<?>> referenced = plan.getValue() == null ? Set.of() : referencedFields(plan.getValue());
        TimeSeriesAggregationFunction function = plan.getAggregation();
        if (function instanceof TimeSeriesAggregationFunction.Standard standard) {
            QueryAggregateFunction aggregate = switch (standard) {
                case COUNT -> QueryAggregateFunction.COUNT;
                case COUNT_DISTINCT -> QueryAggregateFunction.COUNT_DISTINCT;
                case SUM -> QueryAggregateFunction.SUM;
                case AVERAGE -> QueryAggregateFunction.AVERAGE;
                case MIN -> QueryAggregateFunction.MIN;
                case MAX -> QueryAggregateFunction.MAX;
            };
            for (QueryField<?> field : referenced) {
                validateAggregate(field, aggregate);
            }
            return;
        }
        Set<QueryField<?>> allowed = customTimeSeriesAggregations.get(function);
        if (allowed == null || !allowed.containsAll(referenced)) {
            throw new IllegalArgumentException("Time-series aggregation is not allowed for the selected fields");
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
        private final Map<QueryFunctionDefinition<?>, Set<QueryField<?>>> customFunctions = new LinkedHashMap<>();
        private final Set<QueryOperatorDefinition<?, ?>> fieldFreeOperators = new LinkedHashSet<>();
        private final Set<QueryAggregateDefinition<?>> fieldFreeAggregates = new LinkedHashSet<>();
        private final Map<TimeSeriesAggregationFunction, Set<QueryField<?>>> customTimeSeriesAggregations = new LinkedHashMap<>();

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
        public <T> Builder add(QueryField<T> field, Set<? extends QueryOperator> operators) {
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
        public <T> Builder add(QueryField<T> field, Set<? extends QueryOperator> operators, boolean orderable) {
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
                Set<? extends QueryOperator> operators,
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
                Set<? extends QueryOperator> operators,
                boolean orderable,
                Set<QueryFunction> functions,
                Set<? extends QueryAggregateFunction> aggregates
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
            for (QueryAggregateFunction aggregate : copiedAggregates) {
                if (!(aggregate instanceof QueryAggregateFunction.Standard)
                        && !(aggregate instanceof QueryAggregateDefinition<?>)) {
                    throw new IllegalArgumentException("Query aggregate must have a typed definition");
                }
            }
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
         * Allows an application function to reference the specified schema fields.
         *
         * <p>An empty field set permits the function only over field-free arguments.</p>
         *
         * @param definition    the typed function definition
         * @param allowedFields the fields the function may reference
         * @return this builder
         */
        public Builder function(QueryFunctionDefinition<?> definition, Set<QueryField<?>> allowedFields) {
            QueryFunctionDefinition<?> key = Objects.requireNonNull(definition, "definition");
            Set<QueryField<?>> fields = Set.copyOf(Objects.requireNonNull(allowedFields, "allowedFields"));
            if (customFunctions.putIfAbsent(key, fields) != null) {
                throw new IllegalArgumentException("Query function is already registered: " + key.getName());
            }
            return this;
        }

        /**
         * Allows an application operator in conditions that reference no query fields.
         * Field-based conditions are authorized by the field's operator set.
         *
         * @param operator the typed operation definition
         * @return this builder
         */
        public Builder operator(QueryOperatorDefinition<?, ?> operator) {
            fieldFreeOperators.add(Objects.requireNonNull(operator, "operator"));
            return this;
        }

        /**
         * Allows a custom aggregate that references no query fields.
         * Field-based aggregates are authorized by each field's aggregate set.
         *
         * @param aggregate the typed aggregate identity
         * @return this builder
         */
        public Builder aggregate(QueryAggregateDefinition<?> aggregate) {
            fieldFreeAggregates.add(Objects.requireNonNull(aggregate, "aggregate"));
            return this;
        }

        /**
         * Allows an application-selected time-series reduction over the specified fields.
         *
         * @param aggregation   the reduction strategy
         * @param allowedFields the fields the value expression may reference
         * @return this builder
         */
        public Builder timeSeriesAggregation(TimeSeriesAggregationFunction aggregation, Set<QueryField<?>> allowedFields) {
            TimeSeriesAggregationFunction key = Objects.requireNonNull(aggregation, "aggregation");
            if (key instanceof TimeSeriesAggregationFunction.Standard) {
                throw new IllegalArgumentException("Standard time-series reductions use field aggregate permissions");
            }
            if (customTimeSeriesAggregations.putIfAbsent(key, Set.copyOf(allowedFields)) != null) {
                throw new IllegalArgumentException("Time-series aggregation is already registered");
            }
            return this;
        }

        /**
         * Creates the immutable schema.
         *
         * @return the query schema
         */
        public QuerySchema build() {
            for (Set<QueryField<?>> allowedFields : customFunctions.values()) {
                validateAllowedFields(allowedFields);
            }
            for (Set<QueryField<?>> allowedFields : customTimeSeriesAggregations.values()) {
                validateAllowedFields(allowedFields);
            }
            return new QuerySchema(fields, relations, customFunctions, fieldFreeOperators,
                    fieldFreeAggregates, customTimeSeriesAggregations);
        }

        private void validateAllowedFields(Set<QueryField<?>> allowedFields) {
            for (QueryField<?> field : allowedFields) {
                FieldDefinition definition = fields.get(field.getName());
                if (definition == null || !definition.field.equals(field)) {
                    throw new IllegalArgumentException("Query operation references an unknown schema field: " + field.getName());
                }
            }
        }

        private static void validateOperatorTypes(
                QueryField<?> field,
                Set<QueryOperator> operators,
                Set<QueryFunction> functions
        ) {
            for (QueryOperator operator : operators) {
                if (!(operator instanceof QueryOperator.Standard)
                        && !(operator instanceof QueryOperatorDefinition<?, ?>)) {
                    throw new IllegalArgumentException("Query operator must have a typed definition");
                }
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
