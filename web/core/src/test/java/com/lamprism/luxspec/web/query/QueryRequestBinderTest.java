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
import com.lamprism.luxspec.data.query.LogicalCondition;
import com.lamprism.luxspec.data.query.OrderBy;
import com.lamprism.luxspec.data.query.QueryCondition;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.data.query.QueryField;
import com.lamprism.luxspec.data.query.QueryFunction;
import com.lamprism.luxspec.data.query.QueryOperator;
import com.lamprism.luxspec.data.query.QueryOperatorDefinition;
import com.lamprism.luxspec.data.query.QuerySchema;
import com.lamprism.luxspec.web.pagination.DefaultKeysetCursorCodec;
import com.lamprism.luxspec.web.pagination.KeysetCursorCodec;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author RollW
 */
class QueryRequestBinderTest {
    private static final QueryField<String> STATUS = QueryField.of("status", String.class);
    private static final QueryField<Integer> PRIORITY = QueryField.of("priority", Integer.class);

    @Test
    void bindsOnlyExplicitlyRegisteredAndAuthorizedCustomOperators() {
        QueryOperatorDefinition<String, Integer> minimum = QueryOperatorDefinition.of(
                "minimumLength", String.class, Integer.class, 1, 1
        );
        QueryField<String> other = QueryField.of("other", String.class);
        QuerySchema schema = QuerySchema.builder().add(STATUS, Set.of(minimum))
                .add(PRIORITY, Set.of(QueryOperator.EQUAL)).add(other, Set.of(QueryOperator.EQUAL)).build();
        QueryRequestBinder binder = QueryRequestBinder.builder(schema)
                .field(STATUS, QueryParameterParser.string())
                .field(PRIORITY, QueryParameterParser.integer())
                .field(other, QueryParameterParser.string())
                .operator("minLength", minimum, QueryParameterParser.integer())
                .build();

        QueryCriteria criteria = binder.bindCriteria(Map.of("filter.status.minLength", List.of("4")));
        QueryCondition<?, ?> condition = assertInstanceOf(QueryCondition.class, criteria.getCondition());
        assertEquals(minimum, condition.getOperator());
        assertEquals(List.of(4), condition.getValues());
        assertThrows(QueryBindingException.class, () -> binder.bindCriteria(Map.of("filter.status.minLength", List.of("bad"))));
        assertThrows(QueryBindingException.class, () -> binder.bindCriteria(Map.of("filter.status.minLength", List.of("1", "2"))));
        assertThrows(QueryBindingException.class, () -> binder.bindCriteria(Map.of("filter.priority.minLength", List.of("4"))));
        assertThrows(QueryBindingException.class, () -> binder.bindCriteria(Map.of("filter.other.minLength", List.of("4"))));
        assertThrows(QueryBindingException.class, () -> binder.bindCriteria(Map.of("filter.status.unknown", List.of("4"))));
        assertThrows(IllegalArgumentException.class, () -> QueryRequestBinder.builder(schema)
                .operator("eq", minimum, QueryParameterParser.integer()));
    }

    @Test
    void bindsTypedFiltersRepeatedMembershipOrderingAndPageWindow() {
        Map<String, List<String>> parameters = new LinkedHashMap<>();
        parameters.put("filter.status.eq", List.of("active"));
        parameters.put("filter.priority.in", List.of("10", "20"));
        parameters.put("sort", List.of("priority:desc", "status:asc"));
        parameters.put("offset", List.of("2"));
        parameters.put("limit", List.of("10"));
        parameters.put("mode", List.of("page"));
        QueryRequestBinder binder = binder();
        QueryCriteria criteria = binder.bindCriteria(parameters);

        PageWindow window = assertInstanceOf(PageWindow.class, binder.bindWindow(parameters, criteria));
        assertEquals(2L, window.offset());
        assertEquals(10, window.limit());
        LogicalCondition condition = assertInstanceOf(LogicalCondition.class, criteria.getCondition());
        assertEquals(2, condition.getExpressions().size());
        QueryCondition<?, ?> status = assertInstanceOf(QueryCondition.class, condition.getExpressions().get(0));
        assertEquals(List.of("active"), status.getValues());
        QueryCondition<?, ?> priorities = assertInstanceOf(QueryCondition.class, condition.getExpressions().get(1));
        assertEquals(List.of(10, 20), priorities.getValues());
        assertEquals(2, criteria.getOrders().size());
    }

    @Test
    void bindsExplicitNullPlacementForSorting() {
        QueryCriteria criteria = binder().bindCriteria(Map.of("sort", List.of("priority:desc:nullsLast", "status:asc:nullsFirst")));

        assertEquals(OrderBy.NullPlacement.LAST, criteria.getOrders().get(0).getNullPlacement());
        assertEquals(OrderBy.NullPlacement.FIRST, criteria.getOrders().get(1).getNullPlacement());
        assertThrows(QueryBindingException.class,
                () -> binder().bindCriteria(Map.of("sort", List.of("priority:asc:nullsUnknown"))));
    }

    @Test
    void bindsNullPredicatesAndDefaultsToBoundedSlice() {
        Map<String, List<String>> parameters = Map.of("filter.status.isNull", List.of("true"));
        QueryRequestBinder binder = binder();
        QueryCriteria criteria = binder.bindCriteria(parameters);

        SliceWindow window = assertInstanceOf(SliceWindow.class, binder.bindWindow(parameters, criteria));
        assertEquals(0L, window.offset());
        assertEquals(25, window.limit());
        QueryCondition<?, ?> condition = assertInstanceOf(QueryCondition.class, criteria.getCondition());
        assertEquals(QueryOperator.IS_NULL, condition.getOperator());
        assertEquals(List.of(), condition.getValues());
    }

    @Test
    void supportsExplicitJdkParsersWithoutReflectiveConstruction() {
        QueryField<Instant> CREATED_AT = QueryField.of("createdAt", Instant.class);
        QuerySchema schema = QuerySchema.builder()
                .add(CREATED_AT, Set.of(QueryOperator.GREATER_THAN))
                .build();
        QueryCriteria criteria = QueryRequestBinder.builder(schema)
                .field(CREATED_AT, QueryParameterParser.instant())
                .build()
                .bindCriteria(Map.of("filter.createdAt.gt", List.of("2026-01-01T00:00:00Z")));

        ComparisonCondition<?> condition = assertInstanceOf(
                ComparisonCondition.class,
                criteria.getCondition()
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

        Map<String, List<String>> parameters = Map.of(
                "mode", List.of("keyset"),
                "sort", List.of("priority:desc", "status:asc"),
                "limit", List.of("10"),
                "cursor", List.of(cursor)
        );
        QueryCriteria criteria = binder.bindCriteria(parameters);

        KeysetWindow window = assertInstanceOf(KeysetWindow.class, binder.bindWindow(parameters, criteria));
        assertEquals(10, window.limit());
        assertEquals(List.of(20, "active"), window.after().values());

        List<OrderBy<?>> explicitOrders = List.of(
                OrderBy.descending(PRIORITY, OrderBy.NullPlacement.LAST),
                OrderBy.ascending(STATUS, OrderBy.NullPlacement.FIRST));
        String nullableCursor = codec.encode(explicitOrders, new KeysetPosition(Arrays.asList(null, "active")));
        Map<String, List<String>> nullableParameters = Map.of(
                "mode", List.of("keyset"),
                "sort", List.of("priority:desc:nullsLast", "status:asc:nullsFirst"),
                "cursor", List.of(nullableCursor)
        );
        QueryCriteria nullableCriteria = binder.bindCriteria(nullableParameters);
        KeysetWindow nullableWindow = assertInstanceOf(KeysetWindow.class, binder.bindWindow(nullableParameters, nullableCriteria));
        assertEquals(Arrays.asList(null, "active"), nullableWindow.after().values());
        Map<String, List<String>> mismatchedParameters = Map.of(
                "mode", List.of("keyset"),
                "sort", List.of("priority:desc:nullsFirst", "status:asc:nullsFirst"),
                "cursor", List.of(nullableCursor)
        );
        QueryCriteria mismatchedCriteria = binder.bindCriteria(mismatchedParameters);
        assertThrows(QueryBindingException.class, () -> binder.bindWindow(mismatchedParameters, mismatchedCriteria));
    }

    @Test
    void rejectsKeysetCursorsWithoutAnEndpointCodec() {
        Map<String, List<String>> parameters = Map.of("mode", List.of("keyset"), "sort", List.of("priority:desc"));
        QueryRequestBinder binder = binder();
        QueryCriteria criteria = binder.bindCriteria(parameters);
        assertThrows(
                QueryBindingException.class,
                () -> binder.bindWindow(parameters, criteria)
        );
    }

    @Test
    void rejectsUnknownValuesAndUnsafeOrExcessiveRequests() {
        assertThrows(
                QueryBindingException.class,
                () -> binder().bindCriteria(Map.of("filter.unknown.eq", List.of("active")))
        );
        assertThrows(
                QueryBindingException.class,
                () -> binder().bindCriteria(Map.of("filter.status.contains", List.of("active")))
        );
        assertThrows(
                QueryBindingException.class,
                () -> binder().bindCriteria(Map.of("filter.priority.gte", List.of("not-a-number")))
        );
        assertThrows(
                QueryBindingException.class,
                () -> binder().bindWindow(Map.of("limit", List.of("101")), QueryCriteria.empty())
        );
        assertThrows(
                QueryBindingException.class,
                () -> binder().bindWindow(Map.of("unexpected", List.of("x")), QueryCriteria.empty())
        );
        assertThrows(
                QueryBindingException.class,
                () -> binder().bindCriteria(Map.of("filter.status.eq", List.of("active"), "unexpected", List.of("x")))
        );
        assertThrows(
                QueryBindingException.class,
                () -> binder().bindCriteria(Map.of("filter.status.isNull", List.of("false")))
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
                () -> binder.bindCriteria(Map.of("filter.status.isNotNull", List.of()))
        );
        QueryCriteria unauthorized = new QueryCriteria(QueryCondition.equal(PRIORITY, 1), List.of());
        assertThrows(QueryBindingException.class, () -> binder.bindWindow(Map.of(), unauthorized));
    }

    @Test
    void supportsApplicationOwnedBinderImplementations() {
        QueryRequestBinder binder = new QueryRequestBinder() {
            @Override
            public QueryCriteria bindCriteria(Map<String, ? extends List<String>> parameters) {
                return QueryCriteria.empty();
            }

            @Override
            public QueryWindow bindWindow(Map<String, ? extends List<String>> parameters, QueryCriteria criteria) {
                return new SliceWindow(0L, 1);
            }
        };
        Map<String, List<String>> parameters = Map.of("applicationFilter", List.of("active"));
        QueryCriteria criteria = binder.bindCriteria(parameters);
        SliceWindow window = assertInstanceOf(SliceWindow.class, binder.bindWindow(parameters, criteria));
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
