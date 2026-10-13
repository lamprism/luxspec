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
import com.lamprism.luxspec.data.pagination.SliceWindow;
import com.lamprism.luxspec.data.query.ComparisonCondition;
import com.lamprism.luxspec.data.query.LogicalCondition;
import com.lamprism.luxspec.data.query.OrderBy;
import com.lamprism.luxspec.data.query.QueryCondition;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.data.query.QueryField;
import com.lamprism.luxspec.data.query.QueryFunction;
import com.lamprism.luxspec.data.query.QueryOperator;
import com.lamprism.luxspec.data.query.QuerySchema;
import com.lamprism.luxspec.web.pagination.DefaultKeysetCursorCodec;
import com.lamprism.luxspec.web.pagination.KeysetCursorCodec;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryRequestBinderTest {
    private static final QueryField<String> STATUS = QueryField.of("status", String.class);
    private static final QueryField<Integer> PRIORITY = QueryField.of("priority", Integer.class);

    @Test
    void bindsTypedFiltersRepeatedMembershipOrderingAndPageWindow() {
        Map<String, List<String>> parameters = new LinkedHashMap<>();
        parameters.put("filter.status.eq", List.of("active"));
        parameters.put("filter.priority.in", List.of("10", "20"));
        parameters.put("sort", List.of("priority:desc", "status:asc"));
        parameters.put("offset", List.of("2"));
        parameters.put("limit", List.of("10"));
        parameters.put("mode", List.of("page"));
        QueryRequest request = binder().bind(parameters);

        assertInstanceOf(PageWindow.class, request.getWindow());
        PageWindow window = (PageWindow) request.getWindow();
        assertEquals(2L, window.offset());
        assertEquals(10, window.limit());
        LogicalCondition condition = assertInstanceOf(LogicalCondition.class, request.getCriteria().getCondition());
        assertEquals(2, condition.getExpressions().size());
        QueryCondition<?> status = assertInstanceOf(QueryCondition.class, condition.getExpressions().get(0));
        assertEquals(List.of("active"), status.getValues());
        QueryCondition<?> priorities = assertInstanceOf(QueryCondition.class, condition.getExpressions().get(1));
        assertEquals(List.of(10, 20), priorities.getValues());
        assertEquals(2, request.getCriteria().getOrders().size());
    }

    @Test
    void bindsNullPredicatesAndDefaultsToBoundedSlice() {
        QueryRequest request = binder().bind(Map.of(
                "filter.status.isNull", List.of("true")
        ));

        assertInstanceOf(SliceWindow.class, request.getWindow());
        SliceWindow window = (SliceWindow) request.getWindow();
        assertEquals(0L, window.offset());
        assertEquals(25, window.limit());
        QueryCondition<?> condition = assertInstanceOf(QueryCondition.class, request.getCriteria().getCondition());
        assertEquals(QueryOperator.IS_NULL, condition.getOperator());
        assertEquals(List.of(), condition.getValues());
    }

    @Test
    void supportsExplicitJdkParsersWithoutReflectiveConstruction() {
        QueryField<Instant> CREATED_AT = QueryField.of("createdAt", Instant.class);
        QuerySchema schema = QuerySchema.builder()
                .add(CREATED_AT, Set.of(QueryOperator.GREATER_THAN))
                .build();
        QueryRequest request = QueryRequestBinder.builder(schema)
                .field(CREATED_AT, QueryParameterParser.instant())
                .build()
                .bind(Map.of("filter.createdAt.gt", List.of("2026-01-01T00:00:00Z")));

        ComparisonCondition<?> condition = assertInstanceOf(
                ComparisonCondition.class,
                request.getCriteria().getCondition()
        );
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), condition.getValue());
    }

    @Test
    void bindsSignedKeysetCursorsOnlyForTheConfiguredOrdering() {
        QueryRequestBinder binder = keysetBinder();
        List<OrderBy<?>> orders = List.of(
                OrderBy.descending(PRIORITY),
                OrderBy.ascending(STATUS)
        );
        KeysetCursorCodec codec = DefaultKeysetCursorCodec.builder(
                        "query",
                        new SecretKeySpec(
                                "luxspec-test-signing-key".getBytes(StandardCharsets.UTF_8),
                                "HmacSHA256"
                        )
                )
                .field(PRIORITY, QueryParameterParser.integer())
                .field(STATUS, QueryParameterParser.string())
                .build();
        String cursor = codec.encode(orders, new KeysetPosition(List.of(20, "active")));

        QueryRequest request = binder.bind(Map.of(
                "mode", List.of("keyset"),
                "sort", List.of("priority:desc", "status:asc"),
                "limit", List.of("10"),
                "cursor", List.of(cursor)
        ));

        KeysetWindow window = assertInstanceOf(KeysetWindow.class, request.getWindow());
        assertEquals(10, window.limit());
        assertEquals(List.of(20, "active"), window.after().values());
    }

    @Test
    void rejectsKeysetCursorsWithoutAnEndpointCodec() {
        assertThrows(
                QueryBindingException.class,
                () -> binder().bind(Map.of(
                        "mode", List.of("keyset"),
                        "sort", List.of("priority:desc")
                ))
        );
    }

    @Test
    void rejectsUnknownValuesAndUnsafeOrExcessiveRequests() {
        assertThrows(
                QueryBindingException.class,
                () -> binder().bind(Map.of("filter.unknown.eq", List.of("active")))
        );
        assertThrows(
                QueryBindingException.class,
                () -> binder().bind(Map.of("filter.status.contains", List.of("active")))
        );
        assertThrows(
                QueryBindingException.class,
                () -> binder().bind(Map.of("filter.priority.gte", List.of("not-a-number")))
        );
        assertThrows(
                QueryBindingException.class,
                () -> binder().bind(Map.of("limit", List.of("101")))
        );
        assertThrows(
                QueryBindingException.class,
                () -> binder().bind(Map.of("filter.status.eq", List.of("active"), "unexpected", List.of("x")))
        );
        assertThrows(
                QueryBindingException.class,
                () -> binder().bind(Map.of("filter.status.isNull", List.of("false")))
        );
    }

    @Test
    void rejectsOperatorsNotAllowedByTheServerSchema() {
        QuerySchema schema = QuerySchema.builder()
                .add(STATUS, Set.of(QueryOperator.EQUAL), true)
                .build();
        QueryRequestBinder binder = QueryRequestBinder.builder(schema)
                .field(STATUS, QueryParameterParser.string())
                .build();

        assertThrows(
                QueryBindingException.class,
                () -> binder.bind(Map.of("filter.status.isNotNull", List.of()))
        );
    }

    @Test
    void supportsApplicationOwnedBinderImplementations() {
        QueryRequestBinder binder = parameters -> new QueryRequest(
                QueryCriteria.empty(),
                new SliceWindow(0L, 1)
        );

        QueryRequest request = binder.bind(Map.of("applicationFilter", List.of("active")));

        assertInstanceOf(SliceWindow.class, request.getWindow());
        SliceWindow window = (SliceWindow) request.getWindow();
        assertEquals(1, window.limit());
    }

    private static QueryRequestBinder binder() {
        QuerySchema schema = QuerySchema.builder()
                .add(
                        STATUS,
                        Set.of(
                                QueryOperator.EQUAL,
                                QueryOperator.IN,
                                QueryOperator.IS_NULL,
                                QueryOperator.IS_NOT_NULL
                        ),
                        true,
                        Set.of(QueryFunction.LOWER)
                )
                .add(
                        PRIORITY,
                        Set.of(QueryOperator.IN, QueryOperator.GREATER_THAN_OR_EQUAL),
                        true
                )
                .build();
        return QueryRequestBinder.builder(schema)
                .field(STATUS, QueryParameterParser.string())
                .field(PRIORITY, QueryParameterParser.integer())
                .limits(new QueryBindingLimits(25, 100, 1_000L, 16, 32))
                .build();
    }

    private static QueryRequestBinder keysetBinder() {
        KeysetCursorCodec codec = DefaultKeysetCursorCodec.builder(
                        "query",
                        new SecretKeySpec(
                                "luxspec-test-signing-key".getBytes(StandardCharsets.UTF_8),
                                "HmacSHA256"
                        )
                )
                .field(PRIORITY, QueryParameterParser.integer())
                .field(STATUS, QueryParameterParser.string())
                .build();
        return QueryRequestBinder.builder(QuerySchema.builder()
                        .add(
                                STATUS,
                                Set.of(
                                        QueryOperator.EQUAL,
                                        QueryOperator.IN,
                                        QueryOperator.IS_NULL,
                                        QueryOperator.IS_NOT_NULL
                                ),
                                true,
                                Set.of(QueryFunction.LOWER)
                        )
                        .add(
                                PRIORITY,
                                Set.of(QueryOperator.IN, QueryOperator.GREATER_THAN_OR_EQUAL),
                                true
                        )
                        .build())
                .field(STATUS, QueryParameterParser.string())
                .field(PRIORITY, QueryParameterParser.integer())
                .limits(new QueryBindingLimits(25, 100, 1_000L, 16, 32))
                .keysetCursorCodec(codec)
                .build();
    }
}
