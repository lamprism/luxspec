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

/**
 * Contains no-count slice pagination metadata.
 *
 * @author RollW
 */
public final class SlicePagination implements CollectionPagination {
    private final long offset;
    private final int limit;
    private final boolean hasNext;

    /**
     * Creates no-count slice metadata.
     *
     * @param offset  the zero-based result offset
     * @param limit   the requested slice size
     * @param hasNext whether another slice is available
     */
    public SlicePagination(long offset, int limit, boolean hasNext) {
        if (offset < 0L || limit < 1) {
            throw new IllegalArgumentException("Invalid slice pagination metadata");
        }
        this.offset = offset;
        this.limit = limit;
        this.hasNext = hasNext;
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
     * Returns the requested slice size.
     *
     * @return the slice size
     */
    public int limit() {
        return limit;
    }

    /**
     * Returns whether another slice is available.
     *
     * @return whether another slice exists
     */
    public boolean hasNext() {
        return hasNext;
    }

    /**
     * Returns the no-count slice mode.
     *
     * @return {@code slice}
     */
    @Override
    public String getMode() {
        return "slice";
    }
}
