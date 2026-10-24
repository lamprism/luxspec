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

import org.jspecify.annotations.Nullable;

/**
 * Contains opaque keyset cursor pagination metadata.
 *
 * @author RollW
 */
public final class KeysetPagination implements CollectionPagination {
    private final int limit;
    private final boolean hasNext;
    private final @Nullable String nextCursor;

    /**
     * Creates keyset pagination metadata.
     *
     * @param limit      the requested page size
     * @param hasNext    whether another page is available
     * @param nextCursor the opaque next cursor, when another page is available
     */
    public KeysetPagination(int limit, boolean hasNext, @Nullable String nextCursor) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }
        if (hasNext && (nextCursor == null || nextCursor.isBlank())) {
            throw new IllegalArgumentException("nextCursor is required when hasNext is true");
        }
        if (!hasNext && nextCursor != null) {
            throw new IllegalArgumentException("nextCursor must be absent when hasNext is false");
        }
        this.limit = limit;
        this.hasNext = hasNext;
        this.nextCursor = nextCursor;
    }

    /**
     * Returns the requested page size.
     *
     * @return the page size
     */
    public int limit() {
        return limit;
    }

    /**
     * Reports whether another page is available.
     *
     * @return true when another page is available
     */
    public boolean hasNext() {
        return hasNext;
    }

    /**
     * Returns the opaque next cursor.
     *
     * @return the cursor, when another page is available
     */
    public @Nullable String nextCursor() {
        return nextCursor;
    }

    /**
     * @return the requested page size
     */
    public int getLimit() {
        return limit;
    }

    /**
     * @return whether another page is available
     */
    public boolean isHasNext() {
        return hasNext;
    }

    /**
     * @return the opaque next cursor, when another page is available
     */
    public @Nullable String getNextCursor() {
        return nextCursor;
    }

    @Override
    public String getMode() {
        return "keyset";
    }
}
