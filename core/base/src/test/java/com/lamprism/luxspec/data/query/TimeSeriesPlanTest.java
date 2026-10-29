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

import com.lamprism.luxspec.data.pagination.SliceResult;
import com.lamprism.luxspec.data.pagination.TimeSeriesWindow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author RollW
 */
class TimeSeriesPlanTest {
    private static final QueryField<Instant> TIME = QueryField.of("time", Instant.class);
    private static final QueryField<Integer> AMOUNT = QueryField.of("amount", Integer.class);
    private static final Instant ORIGIN = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void resamplesAndSumsValuesUsingTheSharedPlan() {
        QueryPlan<TimeSeriesPoint<BigDecimal>> plan = QueryPlan.sum(
                        QueryValue.field(TIME),
                        QueryValue.field(AMOUNT),
                        TimeSeriesBucket.of(Duration.ofHours(1), ORIGIN)
                )
                .build();

        assertInstanceOf(TimeSeriesQueryPlan.class, plan);

        SliceResult<TimeSeriesPoint<BigDecimal>> result = (SliceResult<TimeSeriesPoint<BigDecimal>>) executor()
                .query(plan, new TimeSeriesWindow(ORIGIN, ORIGIN.plus(Duration.ofHours(3)), 10));

        assertEquals(
                List.of(
                        new TimeSeriesPoint<>(ORIGIN, new BigDecimal("30")),
                        new TimeSeriesPoint<>(ORIGIN.plus(Duration.ofHours(1)), new BigDecimal("30"))
                ),
                result.getItems()
        );
    }

    @Test
    void countsRowsAndHonorsThePointLimit() {
        QueryPlan<TimeSeriesPoint<Long>> plan = QueryPlan.count(
                QueryValue.field(TIME),
                TimeSeriesBucket.of(Duration.ofHours(1), ORIGIN)
        ).build();

        SliceResult<TimeSeriesPoint<Long>> result = (SliceResult<TimeSeriesPoint<Long>>) executor()
                .query(plan, new TimeSeriesWindow(ORIGIN, ORIGIN.plus(Duration.ofHours(3)), 1));

        assertEquals(List.of(new TimeSeriesPoint<>(ORIGIN, 2L)), result.getItems());
        assertEquals(true, result.hasNext());
    }

    @Test
    void supportsCalendarBucketsAndApplicationReductions() {
        Instant january = Instant.parse("2026-01-31T12:00:00Z");
        Instant february = Instant.parse("2026-02-02T12:00:00Z");
        TimeSeriesBucket months = timestamp -> timestamp.atZone(ZoneOffset.UTC).toLocalDate()
                .withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        TimeSeriesAggregationFunction squares = (values, hasValue) -> {
            long total = 0;
            for (Object value : values) {
                if (value != null) {
                    total += (long) (Integer) value * (Integer) value;
                }
            }
            return total;
        };
        QueryPlan<TimeSeriesPoint<Long>> plan = QueryPlan.timeSeries(
                Long.class, QueryValue.field(TIME), QueryValue.field(AMOUNT), squares, months
        ).build();
        QuerySchema schema = QuerySchema.builder()
                .add(TIME, Set.of(QueryOperator.EQUAL))
                .add(AMOUNT, Set.of(QueryOperator.EQUAL))
                .timeSeriesAggregation(squares, Set.of(AMOUNT))
                .build();
        InMemoryQueryExecutor<Item> executor = InMemoryQueryExecutor.<Item>builder(
                        () -> List.of(new Item(january, 2), new Item(february, 3))
                )
                .field(TIME, Item::time).field(AMOUNT, Item::amount).build();

        schema.validate(plan, QueryComplexityBudget.defaults());
        assertEquals(List.of(new TimeSeriesPoint<>(ORIGIN, 4L),
                        new TimeSeriesPoint<>(Instant.parse("2026-02-01T00:00:00Z"), 9L)),
                executor.query(plan, new TimeSeriesWindow(january, february, 10)).getItems());
        QuerySchema denied = QuerySchema.builder()
                .add(TIME, Set.of(QueryOperator.EQUAL)).add(AMOUNT, Set.of(QueryOperator.EQUAL)).build();
        assertThrows(IllegalArgumentException.class, () -> denied.validate(plan, QueryComplexityBudget.defaults()));
        assertThrows(IllegalArgumentException.class, () -> QueryPlan.timeSeries(
                Long.class, QueryValue.field(TIME), QueryValue.count(), squares, months));
    }

    @Test
    void requiresFieldAggregatePermissionForStandardTimeSeriesReductions() {
        QueryPlan<TimeSeriesPoint<BigDecimal>> plan = QueryPlan.sum(
                QueryValue.field(TIME), QueryValue.field(AMOUNT), TimeSeriesBucket.of(Duration.ofHours(1))
        ).build();
        QuerySchema denied = QuerySchema.builder()
                .add(TIME, Set.of(QueryOperator.EQUAL)).add(AMOUNT, Set.of(QueryOperator.EQUAL)).build();
        QuerySchema allowed = QuerySchema.builder().add(TIME, Set.of(QueryOperator.EQUAL))
                .add(AMOUNT, Set.of(QueryOperator.EQUAL), false, Set.of(), Set.of(QueryAggregateFunction.SUM)).build();

        assertThrows(IllegalArgumentException.class, () -> denied.validate(plan, QueryComplexityBudget.defaults()));
        allowed.validate(plan, QueryComplexityBudget.defaults());
    }

    @Test
    void reducesValuesInTimestampOrderRegardlessOfCandidateOrder() {
        Instant early = ORIGIN.plusSeconds(60);
        Instant late = ORIGIN.plusSeconds(120);
        TimeSeriesAggregationFunction last = (values, hasValue) -> values.get(values.size() - 1);
        QueryPlan<TimeSeriesPoint<Integer>> plan = QueryPlan.timeSeries(
                Integer.class, QueryValue.field(TIME), QueryValue.field(AMOUNT), last,
                TimeSeriesBucket.of(Duration.ofHours(1), ORIGIN)
        ).build();
        InMemoryQueryExecutor<Item> executor = InMemoryQueryExecutor.<Item>builder(
                        () -> List.of(new Item(late, 9), new Item(early, 2))
                )
                .field(TIME, Item::time).field(AMOUNT, Item::amount).build();

        assertEquals(List.of(new TimeSeriesPoint<>(ORIGIN, 9)),
                executor.query(plan, new TimeSeriesWindow(early, late, 10)).getItems());
    }

    private static InMemoryQueryExecutor<Item> executor() {
        return InMemoryQueryExecutor.<Item>builder(() -> List.of(
                        new Item(ORIGIN.plus(Duration.ofMinutes(5)), 10),
                        new Item(ORIGIN.plus(Duration.ofMinutes(25)), 20),
                        new Item(ORIGIN.plus(Duration.ofHours(1)).plus(Duration.ofMinutes(5)), 30),
                        new Item(ORIGIN.minus(Duration.ofMinutes(5)), 99)
                ))
                .field(TIME, Item::time)
                .field(AMOUNT, Item::amount)
                .build();
    }

    private static final class Item {
        private final Instant time;
        private final Integer amount;

        private Item(Instant time, Integer amount) {
            this.time = time;
            this.amount = amount;
        }

        private Instant time() {
            return time;
        }

        private Integer amount() {
            return amount;
        }
    }
}
