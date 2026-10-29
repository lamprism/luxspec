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

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Represents a bounded keyset page and its optional typed continuation position.
 *
 * @param <T> the element type
 *
 * @author RollW
 */
public final class KeysetResult<T> implements QueryResult<T> {
    private final List<T> items;
    private final int limit;
    private final @Nullable KeysetPosition nextPosition;

    /**
     * Creates an immutable keyset result.
     *
     * @param items        the page items
     * @param limit        the requested page limit
     * @param nextPosition the optional continuation position
     */
    public KeysetResult(
            List<T> items,
            int limit,
            @Nullable KeysetPosition nextPosition
    ) {
        Objects.requireNonNull(items, "items");
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }
        List<T> copiedItems = List.copyOf(items);
        if (copiedItems.size() > limit) {
            throw new IllegalArgumentException("keyset items must not exceed the requested limit");
        }
        if (nextPosition != null && copiedItems.isEmpty()) {
            throw new IllegalArgumentException("next position requires at least one item");
        }
        this.items = copiedItems;
        this.limit = limit;
        this.nextPosition = nextPosition;
    }

    /**
     * Returns the requested page limit.
     *
     * @return the page limit
     */
    public int limit() {
        return limit;
    }

    /**
     * Reports whether another keyset page is available.
     *
     * @return true when a continuation position is present
     */
    public boolean hasNext() {
        return nextPosition != null;
    }

    /**
     * Returns the typed continuation position.
     *
     * @return the next position, when another page is available
     */
    public @Nullable KeysetPosition nextPosition() {
        return nextPosition;
    }

    @Override
    public List<T> getItems() {
        return items;
    }
}
