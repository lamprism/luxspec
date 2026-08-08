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

package com.lamprism.luxspec.data.pagination;

import java.util.List;

/**
 * Represents an offset page with an exact total count.
 *
 * @param <T> the element type
 * @author RollW
 */
public final class PageResult<T> implements QueryResult<T> {
    private final List<T> items;
    private final long offset;
    private final int limit;
    private final long total;

    /**
     * Creates a page result with an exact total.
     *
     * @param items  the page items
     * @param offset the zero-based result offset
     * @param limit  the requested page limit
     * @param total  the exact total matching item count
     */
    public PageResult(List<T> items, long offset, int limit, long total) {
        QueryWindows.requireRange(offset, limit);
        if (total < 0L) {
            throw new IllegalArgumentException("total must not be negative");
        }
        List<T> copiedItems = List.copyOf(items);
        if (copiedItems.size() > limit) {
            throw new IllegalArgumentException("page items must not exceed the requested limit");
        }
        if (total < copiedItems.size()) {
            throw new IllegalArgumentException("total must not be less than the returned item count");
        }
        this.items = copiedItems;
        this.offset = offset;
        this.limit = limit;
        this.total = total;
    }

    /**
     * Returns the zero-based result offset.
     *
     * @return the offset
     */
    public long offset() {
        return offset;
    }

    /**
     * Returns the requested page limit.
     *
     * @return the limit
     */
    public int limit() {
        return limit;
    }

    /**
     * Returns the exact total matching item count.
     *
     * @return the total count
     */
    public long total() {
        return total;
    }

    /**
     * Returns immutable page items.
     *
     * @return the page items
     */
    @Override
    public List<T> getItems() {
        return items;
    }
}
