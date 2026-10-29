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

import com.lamprism.luxspec.data.pagination.KeysetPosition;
import com.lamprism.luxspec.data.pagination.KeysetResult;
import com.lamprism.luxspec.data.pagination.KeysetWindow;
import com.lamprism.luxspec.data.pagination.QueryResult;
import com.lamprism.luxspec.data.pagination.QueryWindow;
import com.lamprism.luxspec.data.pagination.UnboundedWindow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies aggregate and nullable projection semantics.
 *
 * @author RollW
 */
class QueryAggregateReviewTest {
    private static final QueryField<String> NAME = QueryField.of("name", String.class);
    private static final QueryField<Integer> AMOUNT = QueryField.of("amount", Integer.class);
    private static final QueryField<Integer> WEIGHT = QueryField.of("weight", Integer.class);

    @Test
    void ignoresNullOperandsWhenCountingAndReducing() {
        InMemoryQueryExecutor<Integer> executor = InMemoryQueryExecutor.<Integer>builder(() -> List.of(1, -1, 2))
                .field(AMOUNT, value -> value < 0 ? null : value)
                .build();
        QueryPlan<List<Object>> plan = QueryPlan.builder(QueryProjection.of(List.of(
                QueryValue.count(QueryValue.field(AMOUNT)),
                QueryValue.countDistinct(QueryValue.field(AMOUNT)),
                QueryValue.sum(QueryValue.field(AMOUNT)),
                QueryValue.average(QueryValue.field(AMOUNT)),
                QueryValue.min(QueryValue.field(AMOUNT)),
                QueryValue.max(QueryValue.field(AMOUNT))
        ), List::copyOf)).build();

        assertEquals(List.of(List.of(2L, 2L, new BigDecimal("3"), 1.5d, 1, 2)),
                executor.query(plan, UnboundedWindow.getInstance()).getItems());
    }

    @Test
    void passesNullableSelectedValuesToProjectionMappers() {
        InMemoryQueryExecutor<String> executor = InMemoryQueryExecutor.<String>builder(() -> List.of("present", "missing"))
                .field(NAME, value -> value.equals("missing") ? null : value)
                .build();
        QueryPlan<String> plan = QueryPlan.builder(QueryProjection.of(List.of(QueryValue.field(NAME)),
                values -> values.get(0) == null ? "absent" : values.get(0).toString())).build();

        assertEquals(List.of("present", "absent"), executor.query(plan, UnboundedWindow.getInstance()).getItems());
        QueryPlan<Optional<String>> optional = QueryPlan.builder(QueryProjection.optional(QueryValue.field(NAME))).build();
        assertEquals(List.of(Optional.of("present"), Optional.empty()),
                executor.query(optional, UnboundedWindow.getInstance()).getItems());
    }

    @Test
    void authorizesAndExecutesMultiOperandApplicationAggregates() {
        QueryAggregateDefinition<Long> weighted = QueryAggregateDefinition.of(
                "weighted", Long.class, Integer.class, Integer.class
        );
        QueryValue<Long> value = QueryValue.aggregate(weighted, QueryValue.field(AMOUNT), QueryValue.field(WEIGHT));
        QueryPlan<Long> plan = QueryPlan.builder(value)
                .having(QueryValueCondition.greaterThan(value, 5L)).build();
        QuerySchema schema = QuerySchema.builder()
                .add(AMOUNT, Set.of(QueryOperator.GREATER_THAN), false, Set.of(), Set.of(weighted))
                .add(WEIGHT, Set.of(QueryOperator.GREATER_THAN), false, Set.of(), Set.of(weighted))
                .build();
        InMemoryQueryExecutor<Integer> executor = InMemoryQueryExecutor.<Integer>builder(() -> List.of(1, 2, -1))
                .field(AMOUNT, candidate -> candidate < 0 ? null : candidate)
                .field(WEIGHT, candidate -> 2)
                .aggregate(weighted, rows -> {
                    long sum = 0;
                    for (List<Object> row : rows) {
                        if (row.get(0) != null && row.get(1) != null) {
                            sum += (long) (Integer) row.get(0) * (Integer) row.get(1);
                        }
                    }
                    return sum;
                })
                .build();

        schema.validate(plan, QueryComplexityBudget.defaults());
        assertEquals(List.of(6L), executor.query(plan, UnboundedWindow.getInstance()).getItems());
        QuerySchema denied = QuerySchema.builder()
                .add(AMOUNT, Set.of(QueryOperator.GREATER_THAN), false, Set.of(), Set.of(weighted))
                .add(WEIGHT, Set.of(QueryOperator.GREATER_THAN))
                .build();
        assertThrows(IllegalArgumentException.class, () -> denied.validate(plan, QueryComplexityBudget.defaults()));
        InMemoryQueryExecutor<Integer> missing = InMemoryQueryExecutor.<Integer>builder(List::of)
                .field(AMOUNT, candidate -> candidate).field(WEIGHT, candidate -> candidate).build();
        assertThrows(IllegalArgumentException.class, () -> missing.query(plan, UnboundedWindow.getInstance()));
    }

    @Test
    void rejectsUnsupportedAggregateShapesAndRequiresFieldFreeAuthorization() {
        QueryAggregateDefinition<Long> rows = QueryAggregateDefinition.of("rows", Long.class);
        QueryPlan<Long> plan = QueryPlan.builder(QueryValue.aggregate(rows)).build();
        assertThrows(IllegalArgumentException.class,
                () -> QuerySchema.builder().build().validate(plan, QueryComplexityBudget.defaults()));
        QuerySchema.builder().aggregate(rows).build().validate(plan, QueryComplexityBudget.defaults());
        InMemoryQueryExecutor<Integer> executor = InMemoryQueryExecutor.<Integer>builder(List::of)
                .aggregate(rows, values -> (long) values.size()).build();
        assertEquals(List.of(0L), executor.query(plan, UnboundedWindow.getInstance()).getItems());
        assertThrows(IllegalArgumentException.class, () -> QueryValue.aggregate(rows, QueryValue.field(AMOUNT)));
        QueryAggregateDefinition<Long> typed = QueryAggregateDefinition.of("typed", Long.class, Integer.class);
        assertThrows(IllegalArgumentException.class, () -> QueryValue.aggregate(typed, QueryValue.literal("wrong")));
        assertThrows(IllegalArgumentException.class, () -> QueryValue.count(QueryValue.count()));
        assertThrows(NullPointerException.class, () -> QueryValue.count(null));
        assertThrows(IllegalArgumentException.class,
                () -> new QueryCriteria(QueryValueCondition.equal(QueryValue.count(), 1L), List.of()));
    }

    @Test
    void mapsNullAggregatesFromAnEmptyGroup() {
        InMemoryQueryExecutor<Integer> executor = InMemoryQueryExecutor.<Integer>builder(List::of)
                .field(AMOUNT, value -> value).build();
        QueryPlan<Optional<BigDecimal>> plan = QueryPlan.builder(
                QueryProjection.optional(QueryValue.sum(QueryValue.field(AMOUNT)))
        ).build();

        assertEquals(List.of(Optional.empty()), executor.query(plan, UnboundedWindow.getInstance()).getItems());
    }

    @Test
    void continuesKeysetsAcrossNullOrderValues() {
        InMemoryQueryExecutor<String> executor = InMemoryQueryExecutor.<String>builder(
                        () -> List.of("null2", "b", "null1", "a"))
                .field(NAME, value -> value)
                .field(QueryField.of("optional", String.class), value -> value.startsWith("null") ? null : value)
                .build();
        QueryField<String> optional = QueryField.of("optional", String.class);
        QueryCriteria criteria = new QueryCriteria(QueryExpression.all(),
                List.of(OrderBy.ascending(optional), OrderBy.ascending(NAME)));
        KeysetResult<String> first = (KeysetResult<String>) executor.query(criteria, new KeysetWindow(2));
        KeysetResult<String> second = (KeysetResult<String>) executor.query(criteria, new KeysetWindow(1, first.nextPosition()));
        KeysetResult<String> third = (KeysetResult<String>) executor.query(criteria, new KeysetWindow(1, second.nextPosition()));

        assertEquals(List.of("a", "b"), first.getItems());
        assertEquals(List.of("null1"), second.getItems());
        assertEquals(List.of("null2"), third.getItems());
        assertNull(third.nextPosition());
    }

    @Test
    void appliesEachNullPlacementToOrderingAndKeysetProjections() {
        QueryField<String> optional = QueryField.of("optional", String.class);
        InMemoryQueryExecutor<String> executor = InMemoryQueryExecutor.<String>builder(
                        () -> List.of("null2", "b", "null1", "a"))
                .field(NAME, value -> value)
                .field(optional, value -> value.startsWith("null") ? null : value)
                .build();
        for (OrderBy.Direction direction : OrderBy.Direction.values()) {
            for (OrderBy.NullPlacement placement : OrderBy.NullPlacement.values()) {
                QueryCriteria criteria = new QueryCriteria(QueryExpression.all(),
                        List.of(new OrderBy<>(optional, direction, placement), OrderBy.ascending(NAME)));
                List<String> nonNull = direction == OrderBy.Direction.ASCENDING ? List.of("a", "b") : List.of("b", "a");
                List<String> expected = new ArrayList<>();
                if (placement == OrderBy.NullPlacement.FIRST) {
                    expected.addAll(List.of("null1", "null2"));
                }
                expected.addAll(nonNull);
                if (placement == OrderBy.NullPlacement.LAST) {
                    expected.addAll(List.of("null1", "null2"));
                }
                assertEquals(expected, executor.query(criteria, UnboundedWindow.getInstance()).getItems());
                QueryPlan<String> plan = QueryPlan.builder(QueryValue.field(NAME)).criteria(criteria).build();
                List<String> paged = new ArrayList<>();
                KeysetPosition position = null;
                for (int index = 0; index < expected.size(); index++) {
                    KeysetResult<String> page = (KeysetResult<String>) executor.query(plan, new KeysetWindow(1, position));
                    paged.addAll(page.getItems());
                    position = page.nextPosition();
                    if (index < expected.size() - 1) {
                        assertNotNull(position);
                    } else {
                        assertNull(position);
                    }
                }
                assertEquals(expected, paged);
            }
        }
    }

    @Test
    void validatesKeysetValueTypesEvenWhenThereAreNoCandidates() {
        InMemoryQueryExecutor<String> executor = InMemoryQueryExecutor.<String>builder(List::of)
                .field(NAME, value -> value).build();
        QueryCriteria criteria = new QueryCriteria(QueryExpression.all(), List.of(OrderBy.ascending(NAME)));

        assertThrows(IllegalArgumentException.class,
                () -> executor.query(criteria, new KeysetWindow(1, new KeysetPosition(List.of(1)))));
    }

    @Test
    void allowsApplicationOwnedQueryShapesAndRejectsThemInStandardExecutors() {
        QueryExecutor<String> custom = new QueryExecutor<>() {
            @Override
            public QueryResult<String> query(QueryCriteria criteria, QueryWindow window) {
                return new CustomResult();
            }

            @Override
            @SuppressWarnings("unchecked")
            public <R> QueryResult<R> query(QueryPlan<R> plan, QueryWindow window) {
                if (!(plan instanceof CustomPlan)) {
                    throw new IllegalArgumentException("Unsupported query plan");
                }
                return (QueryResult<R>) new CustomResult();
            }
        };
        assertEquals(List.of("custom"), custom.query(QueryCriteria.empty(), new CustomWindow()).getItems());
        assertEquals(List.of("custom"), custom.query(new CustomPlan(), new CustomWindow()).getItems());
        InMemoryQueryExecutor<String> standard = InMemoryQueryExecutor.<String>builder(List::of).build();
        assertThrows(IllegalArgumentException.class,
                () -> standard.query(QueryCriteria.empty(), new CustomWindow()));
        assertThrows(IllegalArgumentException.class,
                () -> standard.query(new CustomPlan(), UnboundedWindow.getInstance()));
    }

    @Test
    void allowsApplicationNodesButRejectsThemInTheStandardSchemaAndExecutor() {
        QueryCriteria criteria = new QueryCriteria(new CustomExpression(), List.of());
        QueryValue<Integer> value = new CustomValue();
        QueryPlan<Integer> plan = QueryPlan.builder(value).build();
        QueryExecutor<String> custom = (requested, window) -> {
            if (!(requested.getCondition() instanceof CustomExpression)) {
                throw new IllegalArgumentException("Unsupported expression");
            }
            return new CustomResult();
        };
        assertEquals(List.of("custom"), custom.query(criteria, UnboundedWindow.getInstance()).getItems());

        QuerySchema schema = QuerySchema.builder().build();
        InMemoryQueryExecutor<String> standard = InMemoryQueryExecutor.<String>builder(List::of).build();
        assertThrows(IllegalArgumentException.class, () -> schema.validate(criteria, QueryComplexityBudget.defaults()));
        assertThrows(IllegalArgumentException.class, () -> schema.validate(plan, QueryComplexityBudget.defaults()));
        assertThrows(IllegalArgumentException.class, () -> standard.query(criteria, UnboundedWindow.getInstance()));
        assertThrows(IllegalArgumentException.class, () -> standard.query(plan, UnboundedWindow.getInstance()));
    }

    @Test
    void keepsTheFirstRelationAfterADuplicateRegistrationIsRejected() {
        QueryRelation<String> tags = QueryRelation.toMany("tags", String.class);
        QueryFieldResolver<String> resolver = QueryFieldResolver.<String>builder().field(NAME, value -> value).build();
        Function<List<String>, Iterable<? extends String>> accessor = value -> value;
        InMemoryQueryExecutor.Builder<List<String>> builder = InMemoryQueryExecutor.<List<String>>builder(
                () -> List.of(List.of("alpha"))
        );
        builder.relationToMany(tags, accessor, resolver);
        assertThrows(IllegalArgumentException.class, () -> builder.relationToMany(tags, accessor, resolver));
        QueryCriteria criteria = new QueryCriteria(ExistsCondition.of(tags, QueryCondition.equal(NAME, "alpha")), List.of());
        assertEquals(List.of(List.of("alpha")), builder.build().query(criteria, UnboundedWindow.getInstance()).getItems());
    }

    private static final class CustomWindow implements QueryWindow {
    }

    private static final class CustomExpression implements QueryExpression {
        @Override
        public <R> R accept(QueryExpressionVisitor<R> visitor) {
            return visitor.visitMatchAll();
        }
    }

    private static final class CustomValue implements QueryValue<Integer> {
        @Override
        public Class<Integer> getValueType() {
            return Integer.class;
        }

        @Override
        public <R> R accept(QueryValueVisitor<R> visitor) {
            return QueryValue.literal(1).accept(visitor);
        }
    }

    private static final class CustomPlan implements QueryPlan<String> {
        @Override
        public QueryCriteria getCriteria() {
            return QueryCriteria.empty();
        }
    }

    private static final class CustomResult implements QueryResult<String> {
        @Override
        public List<String> getItems() {
            return List.of("custom");
        }
    }
}
