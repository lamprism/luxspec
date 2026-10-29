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

import com.lamprism.luxspec.data.pagination.UnboundedWindow;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies application-defined query operator authorization and execution.
 *
 * @author RollW
 */
class QueryOperatorExtensionTest {
    private static final QueryField<String> NAME = QueryField.of("name", String.class);
    private static final QueryOperatorDefinition<String, Integer> MIN_LENGTH = QueryOperatorDefinition.of(
            "minimumLength", String.class, Integer.class, 1, 1
    );

    @Test
    void executesCustomOperatorsForFieldsAndNestedValues() {
        QuerySchema schema = QuerySchema.builder()
                .add(NAME, Set.of(MIN_LENGTH), false, Set.of(QueryFunction.LOWER))
                .build();
        InMemoryQueryExecutor<String> executor = InMemoryQueryExecutor.<String>builder(
                        () -> List.of("Alpha", "beta", "x")
                )
                .field(NAME, value -> value)
                .operator(MIN_LENGTH, (value, arguments) -> value != null && value.length() >= arguments.get(0))
                .build();
        QueryCondition<String, Integer> fieldCondition = QueryCondition.of(NAME, MIN_LENGTH, List.of(4));
        List<Integer> arguments = fieldCondition.getValues();
        QueryCriteria direct = new QueryCriteria(fieldCondition, List.of());
        QueryCriteria nested = new QueryCriteria(
                QueryValueCondition.of(QueryValue.lower(NAME), MIN_LENGTH, List.of(4)), List.of()
        );

        schema.validate(direct, QueryComplexityBudget.defaults());
        schema.validate(nested, QueryComplexityBudget.defaults());
        assertEquals(List.of(4), arguments);
        assertEquals(List.of("Alpha", "beta"), executor.query(direct, UnboundedWindow.getInstance()).getItems());
        assertEquals(List.of("Alpha", "beta"), executor.query(nested, UnboundedWindow.getInstance()).getItems());
    }

    @Test
    void requiresAuthorizationAndImplementationForEveryBranch() {
        QueryCriteria custom = new QueryCriteria(QueryCondition.of(NAME, MIN_LENGTH, List.of(4)), List.of());
        QuerySchema denied = QuerySchema.builder().add(NAME, Set.of(QueryOperator.EQUAL)).build();
        assertThrows(IllegalArgumentException.class, () -> denied.validate(custom, QueryComplexityBudget.defaults()));
        InMemoryQueryExecutor<String> empty = InMemoryQueryExecutor.<String>builder(List::of)
                .field(NAME, value -> value).build();
        InMemoryQueryExecutor<String> populated = InMemoryQueryExecutor.<String>builder(() -> List.of("alpha"))
                .field(NAME, value -> value).build();
        QueryCriteria shortCircuit = new QueryCriteria(
                QueryExpression.or(List.of(QueryExpression.all(), custom.getCondition())), List.of()
        );

        assertThrows(IllegalArgumentException.class, () -> empty.query(custom, UnboundedWindow.getInstance()));
        assertThrows(IllegalArgumentException.class, () -> populated.query(shortCircuit, UnboundedWindow.getInstance()));
    }

    @Test
    void delegatesNullSemanticsToTheRegisteredOperator() {
        QueryOperatorDefinition<String, Void> absent = QueryOperatorDefinition.of("absent", String.class, Void.class, 0, 0);
        QueryCriteria criteria = new QueryCriteria(QueryCondition.of(NAME, absent, List.of()), List.of());
        QuerySchema schema = QuerySchema.builder().add(NAME, Set.of(absent)).build();
        InMemoryQueryExecutor<String> executor = InMemoryQueryExecutor.<String>builder(() -> List.of("missing", "value"))
                .field(NAME, value -> value.equals("missing") ? null : value)
                .operator(absent, (value, arguments) -> value == null)
                .build();

        schema.validate(criteria, QueryComplexityBudget.defaults());
        assertEquals(List.of("missing"), executor.query(criteria, UnboundedWindow.getInstance()).getItems());

        QueryCriteria negated = new QueryCriteria(
                QueryExpression.not(QueryCondition.of(NAME, MIN_LENGTH, List.of(3))), List.of()
        );
        InMemoryQueryExecutor<String> lengths = InMemoryQueryExecutor.<String>builder(() -> List.of("missing", "value"))
                .field(NAME, value -> value.equals("missing") ? null : value)
                .operator(MIN_LENGTH, (value, arguments) -> value != null && value.length() >= arguments.get(0))
                .build();
        assertEquals(List.of("missing"), lengths.query(negated, UnboundedWindow.getInstance()).getItems());
    }

    @Test
    void validatesCustomArgumentTypesArityAndBudget() {
        assertThrows(IllegalArgumentException.class, () -> QueryCondition.of(NAME, MIN_LENGTH, List.of()));
        assertThrows(IllegalArgumentException.class, () -> QueryCondition.of(NAME, MIN_LENGTH, List.of(1, 2)));
        assertThrows(IllegalArgumentException.class, () -> mismatchedArgument());
        QueryOperatorDefinition<String, Integer> many = QueryOperatorDefinition.of("many", String.class, Integer.class, 0, 200);
        QueryCriteria criteria = new QueryCriteria(QueryCondition.of(NAME, many, Collections.nCopies(101, 1)), List.of());
        QuerySchema schema = QuerySchema.builder().add(NAME, Set.of(many)).build();
        assertThrows(IllegalArgumentException.class, () -> schema.validate(criteria, QueryComplexityBudget.defaults()));
    }

    @Test
    void requiresExplicitPermissionForFieldFreeCustomConditions() {
        QueryCriteria criteria = new QueryCriteria(
                QueryValueCondition.of(QueryValue.literal("value"), MIN_LENGTH, List.of(4)), List.of()
        );
        QuerySchema denied = QuerySchema.builder().add(NAME, Set.of(MIN_LENGTH)).build();
        QuerySchema allowed = QuerySchema.builder().operator(MIN_LENGTH).build();

        assertThrows(IllegalArgumentException.class, () -> denied.validate(criteria, QueryComplexityBudget.defaults()));
        allowed.validate(criteria, QueryComplexityBudget.defaults());
    }

    @Test
    void executesOperatorsInsideRelatedScopesAndAggregateHaving() {
        QueryRelation<String> tags = QueryRelation.toMany("tags", String.class);
        QueryFieldResolver<String> tagFields = QueryFieldResolver.<String>builder().field(NAME, value -> value).build();
        List<String> matching = List.of("alpha", "x");
        List<String> nonMatching = List.of("x");
        InMemoryQueryExecutor<List<String>> executor = InMemoryQueryExecutor.<List<String>>builder(
                        () -> List.of(matching, nonMatching)
                )
                .relationToMany(tags, value -> value, tagFields)
                .operator(MIN_LENGTH, (value, arguments) -> value != null && value.length() >= arguments.get(0))
                .build();
        QueryCriteria related = new QueryCriteria(
                ExistsCondition.of(tags, QueryCondition.of(NAME, MIN_LENGTH, List.of(4))), List.of()
        );
        QuerySchema.builder().relation(tags, QuerySchema.builder().add(NAME, Set.of(MIN_LENGTH)).build())
                .build().validate(related, QueryComplexityBudget.defaults());
        assertEquals(List.of(matching), executor.query(related, UnboundedWindow.getInstance()).getItems());

        QueryOperatorDefinition<Long, Long> minimumCount = QueryOperatorDefinition.of(
                "minimumCount", Long.class, Long.class, 1, 1
        );
        QueryPlan<Long> plan = QueryPlan.builder(QueryValue.count())
                .having(QueryValueCondition.of(QueryValue.count(), minimumCount, List.of(2L))).build();
        QuerySchema.builder().operator(minimumCount).build().validate(plan, QueryComplexityBudget.defaults());
        InMemoryQueryExecutor<String> aggregateExecutor = InMemoryQueryExecutor.<String>builder(() -> List.of("a", "b"))
                .operator(minimumCount, (value, arguments) -> value != null && value >= arguments.get(0))
                .build();
        assertEquals(List.of(2L), aggregateExecutor.query(plan, UnboundedWindow.getInstance()).getItems());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void mismatchedArgument() {
        QueryCondition.of(NAME, (QueryOperatorDefinition) MIN_LENGTH, List.of("four"));
    }
}
