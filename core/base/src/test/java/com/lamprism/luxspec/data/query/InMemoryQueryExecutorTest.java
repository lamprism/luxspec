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

import com.lamprism.luxspec.data.pagination.KeysetResult;
import com.lamprism.luxspec.data.pagination.KeysetWindow;
import com.lamprism.luxspec.data.pagination.PageResult;
import com.lamprism.luxspec.data.pagination.PageWindow;
import com.lamprism.luxspec.data.pagination.QueryResult;
import com.lamprism.luxspec.data.pagination.SliceResult;
import com.lamprism.luxspec.data.pagination.SliceWindow;
import com.lamprism.luxspec.data.pagination.TimeSeriesWindow;
import com.lamprism.luxspec.data.pagination.UnboundedWindow;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryQueryExecutorTest {
    private static final QueryField<String> NAME = QueryField.of("name", String.class);
    private static final QueryField<Integer> PRIORITY = QueryField.of("priority", Integer.class);
    private static final QueryField<String> CATEGORY = QueryField.of("category", String.class);
    private static final QueryField<String> OPTIONAL_VALUE = QueryField.of("optionalValue", String.class);
    private static final QueryField<String> LABEL = QueryField.of("label", String.class);
    private static final QueryField<Integer> NUMERIC_VALUE = QueryField.of("numericValue", Integer.class);
    private static final QueryField<String> MEMBER_NAME = QueryField.of("memberName", String.class);
    private static final QueryRelation<NestedGroup> GROUPS = QueryRelation.toMany(
            "groups",
            NestedGroup.class
    );
    private static final QueryRelation<NestedMember> MEMBERS = QueryRelation.toMany(
            "members",
            NestedMember.class
    );

    @Test
    void evaluatesTypedConditionsAndLogicalExpressions() {
        QueryExpression condition = QueryExpression.and(List.of(
                QueryExpression.or(List.of(
                        QueryCondition.equal(CATEGORY, "internal"),
                        QueryCondition.equal(NAME, "beta")
                )),
                QueryCondition.notEqual(NAME, "a_b%"),
                QueryCondition.in(CATEGORY, List.of("internal", "external")),
                QueryCondition.notIn(CATEGORY, List.of("restricted")),
                ComparisonCondition.greaterThanOrEqualTo(PRIORITY, 10)
        ));

        QueryResult<Item> result = executor().query(
                new QueryCriteria(condition, List.of()),
                UnboundedWindow.getInstance()
        );

        assertEquals(List.of("alpha", "beta", "gamma"), names(result));
    }

    @Test
    void evaluatesOrderedComparisonsAndLikeEscapes() {
        assertEquals(
                List.of("alpha", "beta"),
                names(query(ComparisonCondition.lessThan(PRIORITY, 30)))
        );
        assertEquals(
                List.of("alpha", "beta", "gamma"),
                names(query(ComparisonCondition.lessThanOrEqualTo(PRIORITY, 30)))
        );
        assertEquals(
                List.of("alpha", "a_b%"),
                names(query(LikeCondition.of(NAME, "a%")))
        );
        assertEquals(
                List.of("a_b%"),
                names(query(LikeCondition.of(NAME, "a\\_b\\%")))
        );
    }

    @Test
    void evaluatesNullPredicates() {
        NullableItem empty = new NullableItem(null);
        NullableItem present = new NullableItem("value");
        InMemoryQueryExecutor<NullableItem> executor = InMemoryQueryExecutor.<NullableItem>builder(
                        () -> List.of(empty, present)
                )
                .field(OPTIONAL_VALUE, NullableItem::value)
                .build();

        QueryResult<NullableItem> nullResult = executor.query(
                new QueryCriteria(QueryCondition.isNull(OPTIONAL_VALUE), List.of()),
                UnboundedWindow.getInstance()
        );
        QueryResult<NullableItem> nonNullResult = executor.query(
                new QueryCriteria(QueryCondition.isNotNull(OPTIONAL_VALUE), List.of()),
                UnboundedWindow.getInstance()
        );

        assertEquals(List.of(empty), nullResult.getItems());
        assertEquals(List.of(present), nonNullResult.getItems());
    }

    @Test
    void evaluatesNestedExistsWithOneRelationResolverContract() {
        NestedOrder matching = new NestedOrder(
                List.of(new NestedGroup(List.of(new NestedMember("target"))))
        );
        NestedOrder notMatching = new NestedOrder(
                List.of(new NestedGroup(List.of(new NestedMember("other"))))
        );
        NestedOrder empty = new NestedOrder(List.of());
        QueryFieldResolver<NestedGroup> groupFields = QueryFieldResolver.<NestedGroup>builder().build();
        QueryFieldResolver<NestedMember> memberFields = QueryFieldResolver.<NestedMember>builder()
                .field(MEMBER_NAME, NestedMember::name)
                .build();
        QueryRelationResolver<NestedGroup> groupRelations = (group, relation) -> {
            if (!MEMBERS.equals(relation)) {
                throw new IllegalArgumentException("Unexpected nested relation: " + relation.getName());
            }
            return group.members();
        };
        InMemoryQueryExecutor<NestedOrder> executor = InMemoryQueryExecutor.<NestedOrder>builder(
                        () -> List.of(matching, notMatching, empty)
                )
                .relationToMany(GROUPS, NestedOrder::groups, groupFields)
                .relationToMany(MEMBERS, groupRelations, memberFields)
                .build();
        QueryExpression condition = ExistsCondition.of(
                GROUPS,
                ExistsCondition.of(MEMBERS, QueryCondition.equal(MEMBER_NAME, "target"))
        );
        QuerySchema memberSchema = QuerySchema.builder()
                .add(MEMBER_NAME, Set.of(QueryOperator.EQUAL))
                .build();
        QuerySchema groupSchema = QuerySchema.builder()
                .relation(MEMBERS, memberSchema)
                .build();
        QuerySchema schema = QuerySchema.builder()
                .relation(GROUPS, groupSchema)
                .build();
        QueryCriteria criteria = new QueryCriteria(condition, List.of());
        schema.validate(criteria, QueryComplexityBudget.defaults());

        QueryResult<NestedOrder> result = executor.query(criteria, UnboundedWindow.getInstance());

        assertEquals(List.of(matching), result.getItems());
    }

    @Test
    void evaluatesLowerUpperCoalesceAndCastValues() {
        FunctionItem empty = new FunctionItem(null, 10);
        FunctionItem present = new FunctionItem("Alpha", 20);
        InMemoryQueryExecutor<FunctionItem> executor = InMemoryQueryExecutor.<FunctionItem>builder(
                        () -> List.of(empty, present)
                )
                .field(LABEL, FunctionItem::label)
                .field(NUMERIC_VALUE, FunctionItem::numericValue)
                .build();

        QueryResult<FunctionItem> lowerResult = executor.query(
                new QueryCriteria(
                        QueryValueCondition.like(QueryValue.lower(LABEL), "a%"),
                        List.of()
                ),
                UnboundedWindow.getInstance()
        );
        QueryResult<FunctionItem> upperResult = executor.query(
                new QueryCriteria(
                        QueryValueCondition.equal(QueryValue.upper(LABEL), "ALPHA"),
                        List.of()
                ),
                UnboundedWindow.getInstance()
        );
        QueryResult<FunctionItem> coalesceResult = executor.query(
                new QueryCriteria(
                        QueryValueCondition.equal(QueryValue.coalesce(QueryValue.field(LABEL), "fallback"), "fallback"),
                        List.of()
                ),
                UnboundedWindow.getInstance()
        );
        QueryResult<FunctionItem> castResult = executor.query(
                new QueryCriteria(
                        QueryValueCondition.like(QueryValue.cast(NUMERIC_VALUE, String.class), "2%"),
                        List.of()
                ),
                UnboundedWindow.getInstance()
        );

        assertEquals(List.of(present), lowerResult.getItems());
        assertEquals(List.of(present), upperResult.getItems());
        assertEquals(List.of(empty), coalesceResult.getItems());
        assertEquals(List.of(present), castResult.getItems());
    }

    @Test
    void ordersAndWindowsTheFilteredCandidateSet() {
        QueryCriteria criteria = new QueryCriteria(
                QueryExpression.all(),
                List.of(OrderBy.descending(PRIORITY), OrderBy.ascending(NAME))
        );

        PageResult<Item> page = (PageResult<Item>) executor().query(
                criteria,
                new PageWindow(1L, 2)
        );
        SliceResult<Item> slice = (SliceResult<Item>) executor().query(
                criteria,
                new SliceWindow(2L, 1)
        );

        assertEquals(List.of("gamma", "beta"), names(page));
        assertEquals(4L, page.total());
        assertEquals(List.of("beta"), names(slice));
        assertTrue(slice.hasNext());
    }

    @Test
    void continuesOrderedResultsWithAKeysetPosition() {
        QueryCriteria criteria = new QueryCriteria(
                QueryExpression.all(),
                List.of(OrderBy.descending(PRIORITY), OrderBy.ascending(NAME))
        );

        KeysetResult<Item> first = (KeysetResult<Item>) executor().query(
                criteria,
                new KeysetWindow(2)
        );
        KeysetResult<Item> second = (KeysetResult<Item>) executor().query(
                criteria,
                new KeysetWindow(2, first.nextPosition())
        );

        assertEquals(List.of("a_b%", "gamma"), names(first));
        assertEquals(List.of("beta", "alpha"), names(second));
        assertTrue(first.hasNext());
        assertFalse(second.hasNext());
        assertEquals(List.of(30, "gamma"), first.nextPosition().values());
    }

    @Test
    void requiresExplicitOrderingForKeysetWindows() {
        assertThrows(
                IllegalArgumentException.class,
                () -> executor().query(QueryCriteria.empty(), new KeysetWindow(2))
        );
    }

    @Test
    void filtersCandidatesBeforeCountingAndWindowing() {
        PageResult<Item> page = (PageResult<Item>) executor().query(
                item -> "internal".equals(item.category()),
                QueryCriteria.empty(),
                new PageWindow(1L, 1)
        );
        SliceResult<Item> slice = (SliceResult<Item>) executor().query(
                item -> "internal".equals(item.category()),
                QueryCriteria.empty(),
                new SliceWindow(2L, 1)
        );

        assertEquals(List.of("gamma"), names(page));
        assertEquals(3L, page.total());
        assertEquals(List.of("a_b%"), names(slice));
        assertFalse(slice.hasNext());
    }

    @Test
    void usesExplicitCustomOrderingWithoutChangingComparisonConditions() {
        InMemoryQueryExecutor<Item> executor = InMemoryQueryExecutor.<Item>builder(
                        () -> List.of(
                                new Item("alpha", 1, "internal"),
                                new Item("Beta", 2, "internal")
                        )
                )
                .field(NAME, Item::name, String.CASE_INSENSITIVE_ORDER)
                .build();

        QueryResult<Item> result = executor.query(
                new QueryCriteria(QueryExpression.all(), List.of(OrderBy.ascending(NAME))),
                UnboundedWindow.getInstance()
        );
        QueryResult<Item> comparisonResult = executor.query(
                new QueryCriteria(ComparisonCondition.greaterThan(NAME, "alpha"), List.of()),
                UnboundedWindow.getInstance()
        );

        assertEquals(List.of("alpha", "Beta"), names(result));
        assertEquals(List.of(), names(comparisonResult));
    }

    @Test
    void executesTimeSeriesWindowThroughTheRegularQueryExecutor() {
        Instant first = Instant.parse("2026-01-01T00:00:00Z");
        InMemoryQueryExecutor<TimeSeriesPoint<String>> executor = timeSeriesExecutor(first);

        QueryResult<TimeSeriesPoint<String>> queryResult = executor.query(
                QueryCriteria.empty(),
                new TimeSeriesWindow(first, first.plusSeconds(2L), 2)
        );

        assertTrue(queryResult instanceof SliceResult<?>);
        SliceResult<TimeSeriesPoint<String>> result = (SliceResult<TimeSeriesPoint<String>>) queryResult;
        assertEquals(List.of("first", "second"), values(result));
        assertTrue(result.hasNext());
    }

    @Test
    void appliesTimeSeriesCriteriaAndUsesTheTimestampField() {
        Instant first = Instant.parse("2026-01-01T00:00:00Z");
        Instant threshold = first.plusSeconds(1L);
        InMemoryQueryExecutor<TimeSeriesPoint<String>> executor = timeSeriesExecutor(first);
        QueryCriteria criteria = new QueryCriteria(
                ComparisonCondition.greaterThanOrEqualTo(TimeSeriesPoint.TIMESTAMP, threshold),
                List.of(OrderBy.descending(TimeSeriesPoint.TIMESTAMP))
        );
        QuerySchema schema = QuerySchema.builder()
                .add(
                        TimeSeriesPoint.TIMESTAMP,
                        Set.of(QueryOperator.GREATER_THAN_OR_EQUAL),
                        true
                )
                .build();
        schema.validate(criteria, QueryComplexityBudget.defaults());

        QueryResult<TimeSeriesPoint<String>> queryResult = executor.query(
                criteria,
                new TimeSeriesWindow(first, first.plusSeconds(2L), 10)
        );

        assertEquals(List.of("third", "second"), values(queryResult));
    }

    @Test
    void rejectsTimeSeriesQueriesWithoutATimestampField() {
        Instant first = Instant.parse("2026-01-01T00:00:00Z");
        InMemoryQueryExecutor<String> executor = InMemoryQueryExecutor.<String>builder(
                        () -> List.of("value")
                )
                .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.query(
                        QueryCriteria.empty(),
                        new TimeSeriesWindow(first, first, 1)
                )
        );
    }

    private static InMemoryQueryExecutor<Item> executor() {
        return InMemoryQueryExecutor.<Item>builder(InMemoryQueryExecutorTest::items)
                .field(NAME, Item::name)
                .field(PRIORITY, Item::priority)
                .field(CATEGORY, Item::category)
                .build();
    }

    private static List<Item> items() {
        return List.of(
                new Item("alpha", 10, "internal"),
                new Item("beta", 20, "external"),
                new Item("gamma", 30, "internal"),
                new Item("a_b%", 40, "internal")
        );
    }

    private static QueryResult<Item> query(QueryExpression expression) {
        return executor().query(
                new QueryCriteria(expression, List.of()),
                UnboundedWindow.getInstance()
        );
    }

    private static List<String> names(QueryResult<Item> result) {
        return result.getItems().stream().map(Item::name).toList();
    }

    private static InMemoryQueryExecutor<TimeSeriesPoint<String>> timeSeriesExecutor(Instant first) {
        return InMemoryQueryExecutor.<TimeSeriesPoint<String>>builder(
                        () -> List.of(
                                new TimeSeriesPoint<>(first.minusSeconds(1L), "before"),
                                new TimeSeriesPoint<>(first.plusSeconds(2L), "third"),
                                new TimeSeriesPoint<>(first, "first"),
                                new TimeSeriesPoint<>(first.plusSeconds(1L), "second")
                        )
                )
                .field(TimeSeriesPoint.TIMESTAMP, TimeSeriesPoint::getTimestamp)
                .build();
    }

    private static List<String> values(QueryResult<TimeSeriesPoint<String>> result) {
        return result.getItems().stream().map(TimeSeriesPoint::getValue).toList();
    }

    private static final class NullableItem {
        private final String value;

        private NullableItem(String value) {
            this.value = value;
        }

        private String value() {
            return value;
        }
    }

    private static final class FunctionItem {
        private final String label;
        private final Integer numericValue;

        private FunctionItem(String label, Integer numericValue) {
            this.label = label;
            this.numericValue = numericValue;
        }

        private String label() {
            return label;
        }

        private Integer numericValue() {
            return numericValue;
        }
    }

    private static final class NestedOrder {
        private final List<NestedGroup> groups;

        private NestedOrder(List<NestedGroup> groups) {
            this.groups = groups;
        }

        private List<NestedGroup> groups() {
            return groups;
        }
    }

    private static final class NestedGroup {
        private final List<NestedMember> members;

        private NestedGroup(List<NestedMember> members) {
            this.members = members;
        }

        private List<NestedMember> members() {
            return members;
        }
    }

    private static final class NestedMember {
        private final String name;

        private NestedMember(String name) {
            this.name = name;
        }

        private String name() {
            return name;
        }
    }

    private static final class Item {
        private final String name;
        private final int priority;
        private final String category;

        private Item(String name, int priority, String category) {
            this.name = name;
            this.priority = priority;
            this.category = category;
        }

        private String name() {
            return name;
        }

        private Integer priority() {
            return priority;
        }

        private String category() {
            return category;
        }
    }
}
