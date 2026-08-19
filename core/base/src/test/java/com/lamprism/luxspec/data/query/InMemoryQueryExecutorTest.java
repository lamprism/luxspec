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
import com.lamprism.luxspec.data.pagination.SliceResult;
import com.lamprism.luxspec.data.pagination.SliceWindow;
import com.lamprism.luxspec.data.pagination.UnboundedWindow;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryQueryExecutorTest {
    private static final QueryField<String> NAME = QueryField.of("name", String.class);
    private static final QueryField<Integer> PRIORITY = QueryField.of("priority", Integer.class);
    private static final QueryField<String> CATEGORY = QueryField.of("category", String.class);

    @Test
    void evaluatesTypedConditionsAndLogicalExpressions() {
        QueryExpression condition = QueryExpressions.and(List.of(
                QueryExpressions.or(List.of(
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
    void ordersAndWindowsTheFilteredCandidateSet() {
        QueryCriteria criteria = new QueryCriteria(
                QueryExpressions.all(),
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
    void usesExplicitCustomOrderingWhenConfigured() {
        InMemoryQueryExecutor<Item> executor = InMemoryQueryExecutor.<Item>builder(
                        () -> List.of(
                                new Item("alpha", 1, "internal"),
                                new Item("Beta", 2, "internal")
                        )
                )
                .field(NAME, Item::name, String.CASE_INSENSITIVE_ORDER)
                .build();

        QueryResult<Item> result = executor.query(
                new QueryCriteria(QueryExpressions.all(), List.of(OrderBy.ascending(NAME))),
                UnboundedWindow.getInstance()
        );

        assertEquals(List.of("alpha", "Beta"), names(result));
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
