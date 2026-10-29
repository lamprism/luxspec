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

package com.lamprism.luxspec.web.query;

import com.lamprism.luxspec.data.pagination.KeysetPosition;
import com.lamprism.luxspec.data.pagination.KeysetWindow;
import com.lamprism.luxspec.data.pagination.PageWindow;
import com.lamprism.luxspec.data.pagination.QueryWindow;
import com.lamprism.luxspec.data.pagination.SliceWindow;
import com.lamprism.luxspec.data.query.ComparisonCondition;
import com.lamprism.luxspec.data.query.LikeCondition;
import com.lamprism.luxspec.data.query.OrderBy;
import com.lamprism.luxspec.data.query.QueryComplexityBudget;
import com.lamprism.luxspec.data.query.QueryCondition;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.data.query.QueryExpression;
import com.lamprism.luxspec.data.query.QueryField;
import com.lamprism.luxspec.data.query.QueryOperator;
import com.lamprism.luxspec.data.query.QueryOperatorDefinition;
import com.lamprism.luxspec.data.query.QuerySchema;
import com.lamprism.luxspec.web.pagination.KeysetCursorCodec;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Binds a narrow, allowlisted HTTP query-parameter map to typed Luxspec query criteria.
 *
 * <p>The accepted filter form is {@code filter.<field>.<operator>}. Operators are named by this
 * class or explicitly registered by the application rather than parsed as Java types or
 * expression class names. Values are converted only by registered field or operator parsers.</p>
 *
 * <p>A sort term uses {@code field:asc} or {@code field:desc} and may append
 * {@code :nullsFirst} or {@code :nullsLast}.</p>
 *
 * @author RollW
 */
public final class DefaultQueryRequestBinder implements QueryRequestBinder {
    private static final String FILTER_PREFIX = "filter.";
    private static final Map<String, QueryOperator.Standard> OPERATORS = operators();
    private static final List<String> RESERVED_PARAMETERS = List.of(
            "offset",
            "limit",
            "mode",
            "sort",
            "cursor"
    );

    private final QuerySchema schema;
    private final Map<String, FieldBinding<?>> fields;
    private final Map<String, CustomOperatorBinding<?, ?>> customOperators;
    private final QueryComplexityBudget complexityBudget;
    private final QueryBindingLimits limits;
    private final @Nullable KeysetCursorCodec keysetCursorCodec;

    private DefaultQueryRequestBinder(
            QuerySchema schema,
            Map<String, FieldBinding<?>> fields,
            Map<String, CustomOperatorBinding<?, ?>> customOperators,
            QueryComplexityBudget complexityBudget,
            QueryBindingLimits limits,
            @Nullable KeysetCursorCodec keysetCursorCodec
    ) {
        this.schema = schema;
        this.fields = Map.copyOf(fields);
        this.customOperators = Map.copyOf(customOperators);
        this.complexityBudget = complexityBudget;
        this.limits = limits;
        this.keysetCursorCodec = keysetCursorCodec;
    }

    /**
     * Starts a binder for one server-defined schema.
     *
     * @param schema the query field and operator allowlist
     * @return the binder builder
     */
    public static Builder builder(QuerySchema schema) {
        return new Builder(schema);
    }

    /**
     * Binds decoded HTTP parameters to validated query criteria.
     *
     * @param parameters parameter names mapped to decoded repeated values
     * @return the validated query criteria
     */
    @Override
    public QueryCriteria bindCriteria(Map<String, ? extends List<String>> parameters) {
        Map<String, List<String>> copiedParameters = boundedParameters(parameters);
        List<QueryExpression> expressions = new ArrayList<>();
        List<OrderBy<?>> orders = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : copiedParameters.entrySet()) {
            String parameter = entry.getKey();
            if (parameter.startsWith(FILTER_PREFIX)) {
                expressions.add(bindFilter(parameter, entry.getValue()));
            } else if ("sort".equals(parameter)) {
                bindSort(entry.getValue(), orders);
            }
        }

        QueryCriteria criteria = new QueryCriteria(combineExpressions(expressions), orders);
        validateCriteria(criteria);
        return criteria;
    }

    @Override
    public QueryWindow bindWindow(Map<String, ? extends List<String>> parameters, QueryCriteria criteria) {
        Map<String, List<String>> copiedParameters = boundedParameters(parameters);
        QueryCriteria nonNullCriteria = Objects.requireNonNull(criteria, "criteria");
        validateCriteria(nonNullCriteria);
        return bindWindow(copiedParameters, nonNullCriteria.getOrders());
    }

    private Map<String, List<String>> boundedParameters(Map<String, ? extends List<String>> parameters) {
        Map<String, List<String>> copiedParameters = copyParameters(parameters);
        if (copiedParameters.size() > limits.getMaximumParameters()) {
            throw failure("query", "Query parameter count exceeds the configured limit");
        }
        int valueCount = copiedParameters.values().stream().mapToInt(List::size).sum();
        if (valueCount > limits.getMaximumValues()) {
            throw failure("query", "Query value count exceeds the configured limit");
        }
        for (String parameter : copiedParameters.keySet()) {
            if (!parameter.startsWith(FILTER_PREFIX) && !RESERVED_PARAMETERS.contains(parameter)) {
                throw failure(parameter, "Unknown query parameter");
            }
        }
        return copiedParameters;
    }

    private void validateCriteria(QueryCriteria criteria) {
        try {
            schema.validate(criteria, complexityBudget);
        } catch (IllegalArgumentException exception) {
            throw failure("query", "Query is not permitted by the configured schema");
        }
    }

    private static QueryExpression combineExpressions(List<QueryExpression> expressions) {
        if (expressions.isEmpty()) {
            return QueryExpression.all();
        }
        if (expressions.size() == 1) {
            return expressions.get(0);
        }
        return QueryExpression.and(expressions);
    }

    /**
     * Builds a query binder.
     */
    public static final class Builder {
        private final QuerySchema schema;
        private final Map<String, FieldBinding<?>> fields = new LinkedHashMap<>();
        private final Map<String, CustomOperatorBinding<?, ?>> customOperators = new LinkedHashMap<>();
        private QueryComplexityBudget complexityBudget = QueryComplexityBudget.defaults();
        private QueryBindingLimits limits = QueryBindingLimits.defaults();
        private @Nullable KeysetCursorCodec keysetCursorCodec;

        private Builder(QuerySchema schema) {
            this.schema = Objects.requireNonNull(schema, "schema");
        }

        /**
         * Registers one typed query-field parser.
         *
         * @param field  the allowlisted query field
         * @param parser the parser for decoded request values
         * @param <T>    the field type
         * @return this builder
         */
        public <T> Builder field(QueryField<T> field, QueryParameterParser<T> parser) {
            QueryField<T> nonNullField = Objects.requireNonNull(field, "field");
            QueryParameterParser<T> nonNullParser = Objects.requireNonNull(parser, "parser");
            FieldBinding<?> previous = fields.putIfAbsent(
                    nonNullField.getName(),
                    new FieldBinding<>(nonNullField, nonNullParser)
            );
            if (previous != null) {
                throw new IllegalArgumentException("Query field parser is already registered: " + nonNullField.getName());
            }
            return this;
        }

        /**
         * Binds an explicit external name to an application-defined query operator.
         * The schema still decides which fields may use the operator.
         *
         * @param name     the external operator name, without dots or whitespace
         * @param operator the typed operation definition
         * @param parser   the argument value parser
         * @param <T>      the operated value type
         * @param <A>      the argument type
         * @return this builder
         */
        public <T, A> Builder operator(
                String name, QueryOperatorDefinition<T, A> operator, QueryParameterParser<A> parser
        ) {
            String key = Objects.requireNonNull(name, "name");
            if (key.isBlank() || key.indexOf('.') >= 0 || key.chars().anyMatch(Character::isWhitespace)
                    || key.chars().anyMatch(Character::isISOControl) || OPERATORS.containsKey(key)) {
                throw new IllegalArgumentException("Invalid or reserved query operator name");
            }
            CustomOperatorBinding<T, A> binding = new CustomOperatorBinding<>(
                    Objects.requireNonNull(operator, "operator"), Objects.requireNonNull(parser, "parser")
            );
            if (customOperators.putIfAbsent(key, binding) != null) {
                throw new IllegalArgumentException("Query operator name is already registered: " + key);
            }
            return this;
        }

        /**
         * Sets the structural query complexity budget.
         *
         * @param complexityBudget the complexity budget
         * @return this builder
         */
        public Builder complexityBudget(QueryComplexityBudget complexityBudget) {
            this.complexityBudget = Objects.requireNonNull(complexityBudget, "complexityBudget");
            return this;
        }

        /**
         * Sets HTTP parameter and offset-window limits.
         *
         * @param limits the binding limits
         * @return this builder
         */
        public Builder limits(QueryBindingLimits limits) {
            this.limits = Objects.requireNonNull(limits, "limits");
            return this;
        }

        /**
         * Sets the HMAC codec used by keyset windows.
         *
         * @param keysetCursorCodec the endpoint keyset cursor codec
         * @return this builder
         */
        public Builder keysetCursorCodec(KeysetCursorCodec keysetCursorCodec) {
            this.keysetCursorCodec = Objects.requireNonNull(keysetCursorCodec, "keysetCursorCodec");
            return this;
        }

        /**
         * Creates the immutable binder.
         *
         * @return the query binder
         */
        public DefaultQueryRequestBinder build() {
            if (fields.isEmpty()) {
                throw new IllegalArgumentException("At least one query field parser is required");
            }
            return new DefaultQueryRequestBinder(
                    schema,
                    fields,
                    customOperators,
                    complexityBudget,
                    limits,
                    keysetCursorCodec
            );
        }
    }

    private QueryExpression bindFilter(String parameter, List<String> rawValues) {
        int separator = parameter.lastIndexOf('.');
        if (separator <= FILTER_PREFIX.length() || separator == parameter.length() - 1) {
            throw failure(parameter, "Filter must use filter.<field>.<operator>");
        }
        String fieldName = parameter.substring(FILTER_PREFIX.length(), separator);
        String operatorName = parameter.substring(separator + 1);
        FieldBinding<?> binding = fields.get(fieldName);
        if (binding == null) {
            throw failure(parameter, "Unknown query field");
        }
        QueryOperator.Standard operator = OPERATORS.get(operatorName);
        if (operator != null) {
            return binding.condition(operator, rawValues, parameter);
        }
        CustomOperatorBinding<?, ?> custom = customOperators.get(operatorName);
        if (custom != null) {
            return custom.condition(binding.field, rawValues, parameter);
        }
        throw failure(parameter, "Unknown query operator");
    }

    private void bindSort(List<String> rawValues, List<OrderBy<?>> orders) {
        if (rawValues.isEmpty()) {
            throw failure("sort", "Sort must contain at least one field");
        }
        for (String rawValue : rawValues) {
            String term = rawValue;
            int separator = term.lastIndexOf(':');
            if (separator <= 0 || separator == term.length() - 1) {
                throw failure("sort", "Sort must use field:asc or field:desc with optional null placement");
            }
            OrderBy.NullPlacement nullPlacement = null;
            String suffix = term.substring(separator + 1);
            if ("nullsFirst".equals(suffix) || "nullsLast".equals(suffix)) {
                nullPlacement = "nullsFirst".equals(suffix)
                        ? OrderBy.NullPlacement.FIRST : OrderBy.NullPlacement.LAST;
                term = term.substring(0, separator);
                separator = term.lastIndexOf(':');
                if (separator <= 0 || separator == term.length() - 1) {
                    throw failure("sort", "Sort must include a field and direction");
                }
            }
            String fieldName = term.substring(0, separator);
            FieldBinding<?> binding = fields.get(fieldName);
            if (binding == null) {
                throw failure("sort", "Unknown query field");
            }
            String direction = term.substring(separator + 1);
            if ("asc".equals(direction)) {
                orders.add(nullPlacement == null ? binding.ascending() : binding.ascending(nullPlacement));
            } else if ("desc".equals(direction)) {
                orders.add(nullPlacement == null ? binding.descending() : binding.descending(nullPlacement));
            } else {
                throw failure("sort", "Sort direction must be asc or desc");
            }
        }
    }

    private QueryWindow bindWindow(
            Map<String, List<String>> parameters,
            List<OrderBy<?>> orders
    ) {
        long offset = parseLong(parameters.get("offset"), "offset", 0L);
        if (offset < 0L || offset > limits.getMaximumOffset()) {
            throw failure("offset", "Offset exceeds the configured range");
        }
        int limit = parseInt(parameters.get("limit"), "limit", limits.getDefaultLimit());
        if (limit < 1 || limit > limits.getMaximumLimit()) {
            throw failure("limit", "Limit exceeds the configured range");
        }
        String mode = single(parameters.get("mode"), "mode", "slice");
        if (!"keyset".equals(mode) && parameters.containsKey("cursor")) {
            throw failure("cursor", "Cursor requires keyset mode");
        }
        return switch (mode) {
            case "page" -> new PageWindow(offset, limit);
            case "slice" -> new SliceWindow(offset, limit);
            case "keyset" -> {
                if (offset != 0L) {
                    throw failure("offset", "Keyset mode does not accept an offset");
                }
                yield keysetWindow(parameters, orders, limit);
            }
            default -> throw failure("mode", "Mode must be page, slice, or keyset");
        };
    }

    private QueryWindow keysetWindow(
            Map<String, List<String>> parameters,
            List<OrderBy<?>> orders,
            int limit
    ) {
        if (keysetCursorCodec == null) {
            throw failure("mode", "Keyset mode is not configured for this endpoint");
        }
        if (orders.isEmpty()) {
            throw failure("sort", "Keyset mode requires explicit ordering");
        }
        String rawCursor = single(parameters.get("cursor"), "cursor", null);
        if (rawCursor == null) {
            return new KeysetWindow(limit);
        }
        try {
            KeysetPosition position = keysetCursorCodec.decode(orders, rawCursor);
            return new KeysetWindow(limit, position);
        } catch (IllegalArgumentException exception) {
            throw failure("cursor", "Cursor is invalid");
        }
    }

    private static Map<String, List<String>> copyParameters(Map<String, ? extends List<String>> parameters) {
        Map<String, List<String>> copied = new LinkedHashMap<>();
        for (Map.Entry<String, ? extends List<String>> entry : Objects.requireNonNull(parameters, "parameters").entrySet()) {
            String parameter = entry.getKey();
            if (parameter == null || parameter.isBlank()) {
                throw failure("query", "Query parameter name must not be blank");
            }
            List<String> values = entry.getValue();
            if (values == null || values.stream().anyMatch(Objects::isNull)) {
                throw failure(parameter, "Query parameter values must not be null");
            }
            copied.put(parameter, List.copyOf(values));
        }
        return copied;
    }

    private static long parseLong(List<String> values, String parameter, long defaultValue) {
        String raw = single(values, parameter, null);
        if (raw == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(raw);
        } catch (RuntimeException exception) {
            throw failure(parameter, "Query parameter has an invalid integer value");
        }
    }

    private static int parseInt(List<String> values, String parameter, int defaultValue) {
        String raw = single(values, parameter, null);
        if (raw == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw);
        } catch (RuntimeException exception) {
            throw failure(parameter, "Query parameter has an invalid integer value");
        }
    }

    private static @Nullable String single(
            @Nullable List<String> values,
            String parameter,
            @Nullable String defaultValue
    ) {
        if (values == null) {
            return defaultValue;
        }
        if (values.size() != 1) {
            throw failure(parameter, "Query parameter must have exactly one value");
        }
        return values.get(0);
    }

    private static QueryBindingException failure(String parameter, String message) {
        return new QueryBindingException(parameter, message);
    }

    private static Map<String, QueryOperator.Standard> operators() {
        Map<String, QueryOperator.Standard> operators = new LinkedHashMap<>();
        operators.put("eq", QueryOperator.EQUAL);
        operators.put("neq", QueryOperator.NOT_EQUAL);
        operators.put("gt", QueryOperator.GREATER_THAN);
        operators.put("gte", QueryOperator.GREATER_THAN_OR_EQUAL);
        operators.put("lt", QueryOperator.LESS_THAN);
        operators.put("lte", QueryOperator.LESS_THAN_OR_EQUAL);
        operators.put("like", QueryOperator.LIKE);
        operators.put("in", QueryOperator.IN);
        operators.put("notIn", QueryOperator.NOT_IN);
        operators.put("isNull", QueryOperator.IS_NULL);
        operators.put("isNotNull", QueryOperator.IS_NOT_NULL);
        return Map.copyOf(operators);
    }

    private static final class FieldBinding<T> {
        private final QueryField<T> field;
        private final QueryParameterParser<T> parser;

        private FieldBinding(QueryField<T> field, QueryParameterParser<T> parser) {
            this.field = field;
            this.parser = parser;
        }

        private QueryExpression condition(QueryOperator.Standard operator, List<String> rawValues, String parameter) {
            return switch (operator) {
                case EQUAL -> QueryCondition.equal(field, parseSingle(rawValues, parameter));
                case NOT_EQUAL -> QueryCondition.notEqual(field, parseSingle(rawValues, parameter));
                case IN -> QueryCondition.in(field, parseMany(rawValues, parameter));
                case NOT_IN -> QueryCondition.notIn(field, parseMany(rawValues, parameter));
                case IS_NULL -> noValue(QueryCondition.isNull(field), rawValues, parameter);
                case IS_NOT_NULL -> noValue(QueryCondition.isNotNull(field), rawValues, parameter);
                case LIKE -> like(rawValues, parameter);
                case GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL ->
                        ordered(operator, parseSingle(rawValues, parameter), parameter);
            };
        }

        private OrderBy<?> ascending() {
            return OrderBy.ascending(field);
        }

        private OrderBy<?> ascending(OrderBy.NullPlacement nullPlacement) {
            return OrderBy.ascending(field, nullPlacement);
        }

        private OrderBy<?> descending() {
            return OrderBy.descending(field);
        }

        private OrderBy<?> descending(OrderBy.NullPlacement nullPlacement) {
            return OrderBy.descending(field, nullPlacement);
        }

        private T parseSingle(List<String> rawValues, String parameter) {
            if (rawValues.size() != 1) {
                throw failure(parameter, "Operator requires exactly one value");
            }
            return parse(rawValues.get(0), parameter);
        }

        private List<T> parseMany(List<String> rawValues, String parameter) {
            if (rawValues.isEmpty()) {
                throw failure(parameter, "Membership operator requires at least one value");
            }
            List<T> values = new ArrayList<>(rawValues.size());
            for (String rawValue : rawValues) {
                values.add(parse(rawValue, parameter));
            }
            return values;
        }

        private QueryExpression like(List<String> rawValues, String parameter) {
            if (field.getValueType() != String.class) {
                throw failure(parameter, "LIKE requires a String query field");
            }
            @SuppressWarnings("unchecked")
            QueryField<String> stringField = (QueryField<String>) field;
            return LikeCondition.of(stringField, (String) parseSingle(rawValues, parameter));
        }

        private QueryExpression noValue(QueryExpression condition, List<String> rawValues, String parameter) {
            if (!rawValues.isEmpty()
                    && !(rawValues.size() == 1 && (rawValues.get(0).isEmpty() || "true".equalsIgnoreCase(rawValues.get(0))))) {
                throw failure(parameter, "Null operators do not accept a value");
            }
            return condition;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private QueryExpression ordered(QueryOperator.Standard operator, T value, String parameter) {
            if (!Comparable.class.isAssignableFrom(field.getValueType())) {
                throw failure(parameter, "Ordered operator requires a Comparable query field");
            }
            QueryField comparableField = field;
            Comparable comparableValue = (Comparable) value;
            return switch (operator) {
                case GREATER_THAN -> ComparisonCondition.greaterThan(comparableField, comparableValue);
                case GREATER_THAN_OR_EQUAL ->
                        ComparisonCondition.greaterThanOrEqualTo(comparableField, comparableValue);
                case LESS_THAN -> ComparisonCondition.lessThan(comparableField, comparableValue);
                case LESS_THAN_OR_EQUAL -> ComparisonCondition.lessThanOrEqualTo(comparableField, comparableValue);
                default -> throw failure(parameter, "Unsupported ordered operator");
            };
        }

        private T parse(String rawValue, String parameter) {
            try {
                T value = parser.parse(rawValue);
                return Objects.requireNonNull(value, "parser result");
            } catch (RuntimeException exception) {
                throw failure(parameter, "Query value has an invalid format");
            }
        }
    }

    private static final class CustomOperatorBinding<T, A> {
        private final QueryOperatorDefinition<T, A> operator;
        private final QueryParameterParser<A> parser;

        private CustomOperatorBinding(QueryOperatorDefinition<T, A> operator, QueryParameterParser<A> parser) {
            this.operator = operator;
            this.parser = parser;
        }

        @SuppressWarnings("unchecked")
        private QueryExpression condition(QueryField<?> field, List<String> rawValues, String parameter) {
            if (!operator.getOperandType().isAssignableFrom(field.getValueType())) {
                throw failure(parameter, "Query operator does not accept this field type");
            }
            List<A> arguments = new ArrayList<>(rawValues.size());
            for (String raw : rawValues) {
                try {
                    arguments.add(Objects.requireNonNull(parser.parse(raw), "parser result"));
                } catch (RuntimeException exception) {
                    throw failure(parameter, "Query value has an invalid format");
                }
            }
            try {
                return QueryCondition.of((QueryField<T>) field, operator, arguments);
            } catch (IllegalArgumentException exception) {
                throw failure(parameter, "Query operator arguments are invalid");
            }
        }
    }
}
