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

package com.lamprism.luxspec.web.collection;

import com.lamprism.luxspec.data.pagination.CompleteResult;
import com.lamprism.luxspec.data.pagination.KeysetResult;
import com.lamprism.luxspec.data.pagination.PageResult;
import com.lamprism.luxspec.data.pagination.QueryResult;
import com.lamprism.luxspec.data.pagination.SliceResult;
import com.lamprism.luxspec.data.query.OrderBy;
import com.lamprism.luxspec.web.pagination.KeysetCursorCodec;

import java.util.List;
import java.util.Objects;

/**
 * Gives Web collection consumers one items field and explicit pagination metadata.
 *
 * @param <T> the item type
 *
 * @author RollW
 */
public final class CollectionResponse<T> {
    private final List<T> items;
    private final CollectionPagination pagination;

    /**
     * Creates a collection response with explicit pagination metadata.
     *
     * @param items      the response items
     * @param pagination the pagination metadata
     */
    public CollectionResponse(List<T> items, CollectionPagination pagination) {
        this.items = List.copyOf(items);
        this.pagination = Objects.requireNonNull(pagination, "pagination");
    }

    /**
     * Returns the immutable response items.
     *
     * @return the response items
     */
    public List<T> items() {
        return items;
    }

    /**
     * Returns the explicit pagination metadata.
     *
     * @return the pagination metadata
     */
    public CollectionPagination pagination() {
        return pagination;
    }

    /**
     * @return the immutable response items
     */
    public List<T> getItems() {
        return items;
    }

    /**
     * @return the collection pagination metadata
     */
    public CollectionPagination getPagination() {
        return pagination;
    }

    /**
     * Creates a complete collection response from ordinary finite items.
     *
     * @param items the response items
     * @param <T>   the item type
     * @return the complete collection response
     */
    public static <T> CollectionResponse<T> from(List<T> items) {
        return new CollectionResponse<>(items, CompletePagination.getInstance());
    }

    /**
     * Converts one provider-independent query result into its matching Web representation.
     *
     * @param result the query result
     * @param <T>    the item type
     * @return the collection response
     */
    public static <T> CollectionResponse<T> from(QueryResult<T> result) {
        Objects.requireNonNull(result, "result");
        if (result instanceof CompleteResult<?>) {
            return from(result.getItems());
        }
        if (result instanceof PageResult<?> pageResult) {
            return new CollectionResponse<>(
                    result.getItems(),
                    new PagePagination(pageResult.offset(), pageResult.limit(), pageResult.total())
            );
        }
        if (result instanceof SliceResult<?> sliceResult) {
            return new CollectionResponse<>(
                    result.getItems(),
                    new SlicePagination(sliceResult.offset(), sliceResult.limit(), sliceResult.hasNext())
            );
        }
        if (result instanceof KeysetResult<?>) {
            throw new IllegalArgumentException(
                    "Keyset results require an explicit cursor codec and ordering"
            );
        }
        throw new IllegalArgumentException("Unsupported QueryResult type");
    }

    /**
     * Converts a keyset result using the configured opaque cursor codec.
     *
     * @param result            the keyset result
     * @param orders            the exact ordering used to execute the result
     * @param keysetCursorCodec the endpoint keyset cursor codec
     * @param <T>               the item type
     * @return the collection response with opaque keyset metadata
     */
    public static <T> CollectionResponse<T> from(
            KeysetResult<T> result,
            List<? extends OrderBy<?>> orders,
            KeysetCursorCodec keysetCursorCodec
    ) {
        KeysetResult<T> nonNullResult = Objects.requireNonNull(result, "result");
        KeysetCursorCodec nonNullKeysetCursorCodec = Objects.requireNonNull(
                keysetCursorCodec,
                "keysetCursorCodec"
        );
        String nextCursor = nonNullResult.nextPosition() == null
                ? null
                : nonNullKeysetCursorCodec.encode(orders, nonNullResult.nextPosition());
        return new CollectionResponse<>(
                nonNullResult.getItems(),
                new KeysetPagination(nonNullResult.limit(), nonNullResult.hasNext(), nextCursor)
        );
    }
}
