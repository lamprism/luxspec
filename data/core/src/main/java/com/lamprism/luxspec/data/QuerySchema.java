package com.lamprism.luxspec.data;

import java.util.LinkedHashMap;
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

    private QuerySchema(Map<String, FieldDefinition> fields) {
        this.fields = Map.copyOf(fields);
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
     * @param limits the role-specific complexity limits
     */
    public void validate(QueryCriteria criteria, QueryComplexityLimits limits) {
        QueryCriteria nonNullCriteria = Objects.requireNonNull(criteria, "criteria");
        QueryComplexityLimits nonNullLimits = Objects.requireNonNull(limits, "limits");
        validateOrders(nonNullCriteria.getOrders());
        validateExpression(nonNullCriteria.getCondition(), nonNullLimits, 1, new ValidationState());
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
            QueryComplexityLimits limits,
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
        throw new IllegalArgumentException("Unsupported query expression");
    }

    private void validateCondition(QueryCondition<?> condition) {
        validateOperator(condition.getField(), condition.getOperator());
    }

    private void validateOperator(QueryField<?> field, QueryOperator operator) {
        FieldDefinition definition = findDefinition(field);
        if (!definition.operators.contains(operator)) {
            throw new IllegalArgumentException("Query operator is not allowed for field: " + field.getName());
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

        private Builder() {
        }

        /**
         * Adds a filterable but non-orderable field.
         *
         * @param field the typed domain field
         * @param operators the supported field operators
         * @param <T> the field value type
         * @return this builder
         */
        public <T> Builder add(QueryField<T> field, Set<QueryOperator> operators) {
            return add(field, operators, false);
        }

        /**
         * Adds a field with its supported operations and ordering capability.
         *
         * @param field the typed domain field
         * @param operators the supported field operators, which may be empty for an ordering-only field
         * @param orderable whether the field may be used for ordering
         * @param <T> the field value type
         * @return this builder
         */
        public <T> Builder add(QueryField<T> field, Set<QueryOperator> operators, boolean orderable) {
            QueryField<T> nonNullField = Objects.requireNonNull(field, "field");
            Set<QueryOperator> copiedOperators = Set.copyOf(Objects.requireNonNull(operators, "operators"));
            if (copiedOperators.isEmpty() && !orderable) {
                throw new IllegalArgumentException("Query fields must support filtering or ordering");
            }
            validateOperatorTypes(nonNullField, copiedOperators);
            FieldDefinition definition = new FieldDefinition(nonNullField, copiedOperators, orderable);
            FieldDefinition previous = fields.putIfAbsent(nonNullField.getName(), definition);
            if (previous != null) {
                throw new IllegalArgumentException("Query schema contains a duplicate field: " + nonNullField.getName());
            }
            return this;
        }

        /**
         * Creates the immutable schema.
         *
         * @return the query schema
         */
        public QuerySchema build() {
            return new QuerySchema(fields);
        }

        private static void validateOperatorTypes(QueryField<?> field, Set<QueryOperator> operators) {
            for (QueryOperator operator : operators) {
                if (operator == QueryOperator.LIKE && field.getValueType() != String.class) {
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
        private final boolean orderable;

        private FieldDefinition(QueryField<?> field, Set<QueryOperator> operators, boolean orderable) {
            this.field = field;
            this.operators = operators;
            this.orderable = orderable;
        }
    }

    private static final class ValidationState {
        private int nodeCount;

        private void recordNode(QueryComplexityLimits limits) {
            nodeCount++;
            if (nodeCount > limits.getMaximumNodes()) {
                throw new IllegalArgumentException("Query expression exceeds the maximum node count");
            }
        }
    }
}
