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
 * Contains exact-total pagination metadata.
 *
 * @author RollW
 */
public final class PagePagination implements CollectionPagination {
    private final long offset;
    private final int limit;
    private final long total;

    /**
     * Creates exact-total page metadata.
     *
     * @param offset the zero-based result offset
     * @param limit  the requested page size
     * @param total  the exact number of matching items
     */
    public PagePagination(long offset, int limit, long total) {
        if (offset < 0L || limit < 1 || total < offset) {
            throw new IllegalArgumentException("Invalid page pagination metadata");
        }
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
     * Returns the requested page size.
     *
     * @return the page size
     */
    public int limit() {
        return limit;
    }

    /**
     * Returns the exact matching item count.
     *
     * @return the total count
     */
    public long total() {
        return total;
    }

    /**
     * Returns the exact-total page mode.
     *
     * @return {@code page}
     */
    @Override
    public String getMode() {
        return "page";
    }
}
