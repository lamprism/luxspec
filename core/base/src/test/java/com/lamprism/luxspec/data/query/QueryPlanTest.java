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

import com.lamprism.luxspec.data.pagination.PageResult;
import com.lamprism.luxspec.data.pagination.PageWindow;
import com.lamprism.luxspec.data.pagination.QueryResult;
import com.lamprism.luxspec.data.pagination.UnboundedWindow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class QueryPlanTest {
    private static final QueryField<String> CATEGORY = QueryField.of("category", String.class);
    private static final QueryField<Integer> AMOUNT = QueryField.of("amount", Integer.class);

    @Test
    void projectsAndAggregatesRows() {
        QueryPlan<Summary> plan = QueryPlan.builder(
                        QueryProjection.of(
                                List.of(QueryValue.count(), QueryValue.sum(QueryValue.field(AMOUNT))),
                                values -> new Summary((Long) values.get(0), (BigDecimal) values.get(1))
                        )
                )
                .build();

        assertInstanceOf(ProjectionQueryPlan.class, plan);

        QueryResult<Summary> result = executor().query(plan, UnboundedWindow.getInstance());

        assertEquals(List.of(new Summary(3L, new BigDecimal("60"))), result.getItems());
    }

    @Test
    void groupsAndAppliesHavingBeforeWindowing() {
        QueryPlan<GroupSummary> plan = QueryPlan.builder(
                        QueryProjection.of(
                                List.of(QueryValue.field(CATEGORY), QueryValue.count()),
                                values -> new GroupSummary((String) values.get(0), (Long) values.get(1))
                        )
                )
                .criteria(new QueryCriteria(QueryExpression.all(), List.of(OrderBy.ascending(CATEGORY))))
                .groupBy(QueryValue.field(CATEGORY))
                .having(QueryValueCondition.greaterThan(QueryValue.count(), 1L))
                .build();

        PageResult<GroupSummary> result = (PageResult<GroupSummary>) executor().query(
                plan,
                new PageWindow(0L, 1)
        );

        assertEquals(List.of(new GroupSummary("internal", 2L)), result.getItems());
        assertEquals(1L, result.total());
    }

    @Test
    void removesDuplicateProjectedValues() {
        QueryPlan<String> plan = QueryPlan.builder(QueryValue.field(CATEGORY))
                .distinct()
                .build();

        QueryResult<String> result = executor().query(plan, UnboundedWindow.getInstance());

        assertEquals(List.of("internal", "external"), result.getItems());
    }

    @Test
    void schemaCanAuthorizeAggregateFunctions() {
        QuerySchema schema = QuerySchema.builder()
                .add(
                        AMOUNT,
                        Set.of(QueryOperator.GREATER_THAN),
                        false,
                        Set.of(),
                        Set.of(QueryAggregateFunction.SUM)
                )
                .build();
        QueryPlan<BigDecimal> plan = QueryPlan.builder(QueryValue.sum(QueryValue.field(AMOUNT))).build();

        schema.validate(plan, QueryComplexityBudget.defaults());
    }

    private static InMemoryQueryExecutor<Item> executor() {
        return InMemoryQueryExecutor.<Item>builder(() -> List.of(
                        new Item("internal", 10),
                        new Item("external", 20),
                        new Item("internal", 30)
                ))
                .field(CATEGORY, Item::category)
                .field(AMOUNT, Item::amount)
                .build();
    }

    private record Item(String category, Integer amount) {
    }

    private record Summary(long count, BigDecimal sum) {
    }

    private record GroupSummary(String category, long count) {
    }
}
