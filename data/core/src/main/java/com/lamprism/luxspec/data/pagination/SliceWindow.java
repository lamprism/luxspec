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

/**
 * Requests an offset range without requiring a count query.
 *
 * @author RollW
 */
public final class SliceWindow implements QueryWindow {
    private final long offset;
    private final int limit;

    /**
     * Creates a slice window.
     *
     * @param offset the zero-based result offset
     * @param limit  the required positive slice limit
     */
    public SliceWindow(long offset, int limit) {
        QueryWindows.requireRange(offset, limit);
        this.offset = offset;
        this.limit = limit;
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
     * Returns the requested slice limit.
     *
     * @return the limit
     */
    public int limit() {
        return limit;
    }
}
