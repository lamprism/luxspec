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

package com.lamprism.luxspec.data.query.inmemory;

import com.lamprism.luxspec.data.pagination.CompleteResult;
import com.lamprism.luxspec.data.pagination.KeysetPosition;
import com.lamprism.luxspec.data.pagination.KeysetResult;
import com.lamprism.luxspec.data.pagination.KeysetWindow;
import com.lamprism.luxspec.data.pagination.PageResult;
import com.lamprism.luxspec.data.pagination.PageWindow;
import com.lamprism.luxspec.data.pagination.QueryResult;
import com.lamprism.luxspec.data.pagination.QueryWindow;
import com.lamprism.luxspec.data.pagination.SliceResult;
import com.lamprism.luxspec.data.pagination.SliceWindow;
import com.lamprism.luxspec.data.pagination.TimeSeriesWindow;
import com.lamprism.luxspec.data.pagination.UnboundedWindow;
import com.lamprism.luxspec.data.query.OrderBy;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.data.query.QueryField;
import com.lamprism.luxspec.data.query.QueryFieldResolver;
import com.lamprism.luxspec.data.query.TimeSeriesPoint;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Applies ordering, continuation, and result-window semantics for the in-memory executor.
 *
 * @param <T> the queried candidate type
 *
 * @author RollW
 */
final class InMemoryQueryWindow<T> {
    private final QueryFieldResolver<T> fieldResolver;

    InMemoryQueryWindow(QueryFieldResolver<T> fieldResolver) {
        this.fieldResolver = Objects.requireNonNull(fieldResolver, "fieldResolver");
    }

    void requireSupported(QueryWindow window) {
        if (!(window instanceof UnboundedWindow) && !(window instanceof PageWindow)
                && !(window instanceof SliceWindow) && !(window instanceof KeysetWindow)
                && !(window instanceof TimeSeriesWindow)) {
            throw new IllegalArgumentException("Unsupported query window: " + window.getClass().getName());
        }
    }

    QueryCriteria effectiveCriteria(QueryCriteria criteria, QueryWindow window) {
        if (!(window instanceof TimeSeriesWindow) || !criteria.getOrders().isEmpty()) {
            return criteria;
        }
        return new QueryCriteria(
                criteria.getCondition(),
                List.of(OrderBy.ascending(TimeSeriesPoint.TIMESTAMP))
        );
    }

    Predicate<? super T> candidateFilter(
            Predicate<? super T> candidateFilter,
            QueryCriteria criteria,
            QueryWindow window
    ) {
        if (window instanceof KeysetWindow keysetWindow) {
            requireKeysetOrdering(criteria, keysetWindow);
            return candidate -> candidateFilter.test(candidate)
                    && after(candidate, criteria.getOrders(), keysetWindow.after());
        }
        if (!(window instanceof TimeSeriesWindow timeSeriesWindow)) {
            return candidateFilter;
        }
        return candidate -> candidateFilter.test(candidate)
                && timeSeriesWindow.contains(timestamp(candidate));
    }

    void order(List<T> candidates, List<OrderBy<?>> orders) {
        Comparator<T> comparator = null;
        for (OrderBy<?> order : orders) {
            Comparator<T> nextComparator = comparator(order);
            comparator = comparator == null
                    ? nextComparator
                    : comparator.thenComparing(nextComparator);
        }
        if (comparator != null) {
            candidates.sort(comparator);
        }
    }

    QueryResult<T> apply(
            List<T> candidates,
            List<OrderBy<?>> orders,
            QueryWindow window
    ) {
        if (window instanceof UnboundedWindow) {
            return new CompleteResult<>(candidates);
        }
        if (window instanceof PageWindow pageWindow) {
            return page(candidates, pageWindow);
        }
        if (window instanceof SliceWindow sliceWindow) {
            return slice(candidates, sliceWindow);
        }
        if (window instanceof KeysetWindow keysetWindow) {
            return keyset(candidates, keysetWindow, orders);
        }
        if (window instanceof TimeSeriesWindow timeSeriesWindow) {
            return timeSeries(candidates, timeSeriesWindow);
        }
        throw new IllegalArgumentException("Unsupported query window: " + window.getClass().getName());
    }

    private Instant timestamp(T candidate) {
        T nonNullCandidate = Objects.requireNonNull(candidate, "candidate");
        Instant timestamp = fieldResolver.resolve(nonNullCandidate, TimeSeriesPoint.TIMESTAMP);
        if (timestamp == null) {
            throw new IllegalStateException("Time-series timestamp field must not resolve to null");
        }
        return timestamp;
    }

    private <V> Comparator<T> comparator(OrderBy<V> order) {
        OrderBy<V> nonNullOrder = Objects.requireNonNull(order, "order");
        return (left, right) -> {
            V leftValue = fieldResolver.resolve(left, nonNullOrder.getField());
            V rightValue = fieldResolver.resolve(right, nonNullOrder.getField());
            if (leftValue == null || rightValue == null) {
                return compareNulls(leftValue, rightValue, nonNullOrder.getNullPlacement());
            }
            return nonNullOrder.getDirection() == OrderBy.Direction.ASCENDING
                    ? fieldResolver.compare(left, right, nonNullOrder.getField())
                    : fieldResolver.compare(right, left, nonNullOrder.getField());
        };
    }

    private static int compareNulls(@Nullable Object left, @Nullable Object right, OrderBy.NullPlacement placement) {
        if (left == null && right == null) {
            return 0;
        }
        int nullFirst = left == null ? -1 : 1;
        return placement == OrderBy.NullPlacement.FIRST ? nullFirst : -nullFirst;
    }

    private PageResult<T> page(List<T> candidates, PageWindow window) {
        int start = startIndex(candidates.size(), window.offset());
        int end = endIndex(start, window.limit(), candidates.size());
        return new PageResult<>(
                candidates.subList(start, end),
                window.offset(),
                window.limit(),
                candidates.size()
        );
    }

    private SliceResult<T> slice(List<T> candidates, SliceWindow window) {
        int start = startIndex(candidates.size(), window.offset());
        int end = endIndex(start, window.limit(), candidates.size());
        return new SliceResult<>(
                candidates.subList(start, end),
                window.offset(),
                window.limit(),
                end < candidates.size()
        );
    }

    private SliceResult<T> timeSeries(List<T> candidates, TimeSeriesWindow window) {
        int end = Math.min(candidates.size(), window.maximumPoints());
        return new SliceResult<>(
                candidates.subList(0, end),
                0L,
                window.maximumPoints(),
                end < candidates.size()
        );
    }

    private KeysetResult<T> keyset(
            List<T> candidates,
            KeysetWindow window,
            List<OrderBy<?>> orders
    ) {
        int end = Math.min(candidates.size(), window.limit());
        boolean hasNext = end < candidates.size();
        List<T> items = candidates.subList(0, end);
        KeysetPosition nextPosition = hasNext
                ? position(items.get(items.size() - 1), orders)
                : null;
        return new KeysetResult<>(items, window.limit(), nextPosition);
    }

    KeysetPosition position(T item, List<OrderBy<?>> orders) {
        List<@Nullable Object> values = new ArrayList<>(orders.size());
        for (OrderBy<?> order : orders) {
            values.add(value(item, order));
        }
        return new KeysetPosition(values);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private @Nullable Object value(T item, OrderBy<?> order) {
        QueryField field = order.getField();
        return fieldResolver.resolve(item, field);
    }

    private boolean after(
            T candidate,
            List<OrderBy<?>> orders,
            @Nullable KeysetPosition position
    ) {
        if (position == null) {
            return true;
        }
        List<@Nullable Object> values = position.values();
        if (values.size() != orders.size()) {
            throw new IllegalArgumentException("Keyset position does not match query ordering");
        }
        for (int index = 0; index < orders.size(); index++) {
            OrderBy<?> order = orders.get(index);
            int comparison = compare(candidate, order, values.get(index));
            if (comparison == 0) {
                continue;
            }
            return comparison > 0;
        }
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private int compare(T candidate, OrderBy<?> order, @Nullable Object positionValue) {
        QueryField field = order.getField();
        Object candidateValue = fieldResolver.resolve(candidate, field);
        if (candidateValue == null || positionValue == null) {
            return compareNulls(candidateValue, positionValue, order.getNullPlacement());
        }
        return order.getDirection() == OrderBy.Direction.ASCENDING
                ? fieldResolver.compareValues(candidateValue, positionValue, field)
                : fieldResolver.compareValues(positionValue, candidateValue, field);
    }

    private static void requireKeysetOrdering(QueryCriteria criteria, KeysetWindow window) {
        if (criteria.getOrders().isEmpty()) {
            throw new IllegalArgumentException("Keyset windows require explicit query ordering");
        }
        KeysetPosition position = window.after();
        if (position == null) {
            return;
        }
        if (position.values().size() != criteria.getOrders().size()) {
            throw new IllegalArgumentException("Keyset position does not match query ordering");
        }
        for (int index = 0; index < criteria.getOrders().size(); index++) {
            Object value = position.values().get(index);
            if (value != null && !criteria.getOrders().get(index).getField().getValueType().isInstance(value)) {
                throw new IllegalArgumentException("Keyset value does not match query field type");
            }
        }
    }

    static int startIndex(int size, long offset) {
        if (offset >= size) {
            return size;
        }
        return (int) offset;
    }

    static int endIndex(int start, int limit, int size) {
        long requestedEnd = (long) start + limit;
        return (int) Math.min(requestedEnd, size);
    }
}
