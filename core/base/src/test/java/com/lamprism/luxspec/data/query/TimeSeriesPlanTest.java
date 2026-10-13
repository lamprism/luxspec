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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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

    private record Item(Instant time, Integer amount) {
    }
}
