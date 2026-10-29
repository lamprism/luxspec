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

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author RollW
 */
class QueryFunctionExtensionTest {
    private static final QueryField<String> NAME = QueryField.of("name", String.class);
    private static final QueryField<String> OTHER = QueryField.of("other", String.class);
    private static final QueryFunctionDefinition<String> TRIM = QueryFunctionDefinition.of(
            "trim", String.class, String.class
    );

    @Test
    void evaluatesNestedRegisteredFunctionsAfterSchemaAuthorization() {
        QueryValue<String> normalized = QueryValue.upper(QueryValue.function(TRIM, QueryValue.field(NAME)));
        QueryCriteria criteria = new QueryCriteria(QueryValueCondition.equal(normalized, "ALPHA"), List.of());
        QuerySchema schema = QuerySchema.builder()
                .add(NAME, Set.of(QueryOperator.EQUAL), false, Set.of(QueryFunction.UPPER))
                .function(TRIM, Set.of(NAME))
                .build();
        InMemoryQueryExecutor<String> executor = InMemoryQueryExecutor.<String>builder(
                        () -> List.of("  alpha ", "beta")
                )
                .field(NAME, value -> value)
                .function(TRIM, arguments -> {
                    Object value = arguments.get(0);
                    return value == null ? null : ((String) value).trim();
                })
                .build();

        schema.validate(criteria, QueryComplexityBudget.defaults());
        assertEquals(List.of("  alpha "), executor.query(criteria, UnboundedWindow.getInstance()).getItems());
    }

    @Test
    void rejectsUnknownFunctionsAndUnauthorizedFields() {
        QueryCriteria nameCriteria = new QueryCriteria(
                QueryValueCondition.equal(QueryValue.function(TRIM, QueryValue.field(NAME)), "x"), List.of()
        );
        QuerySchema missing = QuerySchema.builder().add(NAME, Set.of(QueryOperator.EQUAL)).build();
        assertThrows(IllegalArgumentException.class, () -> missing.validate(nameCriteria, QueryComplexityBudget.defaults()));

        QuerySchema restricted = QuerySchema.builder()
                .add(NAME, Set.of(QueryOperator.EQUAL))
                .add(OTHER, Set.of(QueryOperator.EQUAL))
                .function(TRIM, Set.of(NAME))
                .build();
        QueryCriteria otherCriteria = new QueryCriteria(
                QueryValueCondition.equal(QueryValue.function(TRIM, QueryValue.field(OTHER)), "x"), List.of()
        );
        assertThrows(IllegalArgumentException.class,
                () -> restricted.validate(otherCriteria, QueryComplexityBudget.defaults()));
        InMemoryQueryExecutor<String> missingImplementation = InMemoryQueryExecutor.<String>builder(() -> List.of("x"))
                .field(NAME, value -> value)
                .build();
        assertThrows(IllegalArgumentException.class,
                () -> missingImplementation.query(nameCriteria, UnboundedWindow.getInstance()));
        InMemoryQueryExecutor<String> empty = InMemoryQueryExecutor.<String>builder(List::of)
                .field(NAME, value -> value)
                .build();
        assertThrows(IllegalArgumentException.class,
                () -> empty.query(nameCriteria, UnboundedWindow.getInstance()));
        QueryCriteria shortCircuit = new QueryCriteria(
                QueryExpression.or(List.of(QueryExpression.all(), nameCriteria.getCondition())), List.of()
        );
        assertThrows(IllegalArgumentException.class,
                () -> missingImplementation.query(shortCircuit, UnboundedWindow.getInstance()));
        QueryPlan<String> projection = QueryPlan.builder(QueryValue.function(TRIM, QueryValue.field(NAME))).build();
        assertThrows(IllegalArgumentException.class,
                () -> empty.query(projection, UnboundedWindow.getInstance()));
    }

    @Test
    void checksFunctionSignaturesAndResultTypes() {
        assertThrows(IllegalArgumentException.class, () -> QueryValue.function(TRIM));
        assertThrows(IllegalArgumentException.class, () -> QueryValue.function(TRIM, QueryValue.literal(1)));
        QueryCriteria criteria = new QueryCriteria(
                QueryValueCondition.equal(QueryValue.function(TRIM, QueryValue.field(NAME)), "x"), List.of()
        );
        InMemoryQueryExecutor<String> executor = InMemoryQueryExecutor.<String>builder(() -> List.of("x"))
                .field(NAME, value -> value)
                .function(TRIM, arguments -> null)
                .build();
        assertEquals(List.of(), executor.query(criteria, UnboundedWindow.getInstance()).getItems());
    }

    @Test
    void countsNestedValueExpressionsAgainstTheComplexityBudget() {
        QueryValue<String> value = QueryValue.field(NAME);
        for (int index = 0; index < 100; index++) {
            value = QueryValue.upper(value);
        }
        QueryCriteria criteria = new QueryCriteria(QueryValueCondition.equal(value, "X"), List.of());
        QuerySchema schema = QuerySchema.builder()
                .add(NAME, Set.of(QueryOperator.EQUAL), false, Set.of(QueryFunction.UPPER))
                .build();

        assertThrows(IllegalArgumentException.class, () -> schema.validate(criteria, QueryComplexityBudget.defaults()));
    }

    @Test
    void countsEveryExpressionNodeAgainstTheComplexityBudget() {
        QueryCriteria criteria = new QueryCriteria(
                QueryExpression.and(java.util.Collections.nCopies(101, QueryExpression.all())), List.of()
        );
        QuerySchema schema = QuerySchema.builder().add(NAME, Set.of(QueryOperator.EQUAL)).build();

        assertThrows(IllegalArgumentException.class, () -> schema.validate(criteria, QueryComplexityBudget.defaults()));
    }

    @Test
    void evaluatesCustomFunctionsInsideAggregates() {
        QueryValue<String> trimmed = QueryValue.function(TRIM, QueryValue.field(NAME));
        QueryPlan<Long> plan = QueryPlan.builder(QueryValue.count(trimmed)).build();
        QuerySchema schema = QuerySchema.builder()
                .add(NAME, Set.of(QueryOperator.EQUAL), false, Set.of(), Set.of(QueryAggregateFunction.COUNT))
                .function(TRIM, Set.of(NAME))
                .build();
        InMemoryQueryExecutor<String> executor = InMemoryQueryExecutor.<String>builder(() -> List.of(" a ", " b "))
                .field(NAME, value -> value)
                .function(TRIM, arguments -> ((String) arguments.get(0)).trim())
                .build();

        schema.validate(plan, QueryComplexityBudget.defaults());
        assertEquals(List.of(2L), executor.query(plan, UnboundedWindow.getInstance()).getItems());
    }

    @Test
    void recognizesAggregatesNestedInsideCustomFunctions() {
        QueryFunctionDefinition<Long> identity = QueryFunctionDefinition.of("identity", Long.class, Long.class);
        QueryPlan<Long> plan = QueryPlan.builder(QueryValue.function(identity, QueryValue.count())).build();
        QuerySchema schema = QuerySchema.builder().function(identity, Set.of()).build();
        InMemoryQueryExecutor<String> executor = InMemoryQueryExecutor.<String>builder(() -> List.of("a", "b"))
                .function(identity, arguments -> (Long) arguments.get(0))
                .build();

        schema.validate(plan, QueryComplexityBudget.defaults());
        assertEquals(List.of(2L), executor.query(plan, UnboundedWindow.getInstance()).getItems());
    }
}
