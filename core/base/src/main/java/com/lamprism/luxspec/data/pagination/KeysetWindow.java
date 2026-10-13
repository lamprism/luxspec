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

/**
 * Requests a bounded keyset page after an optional ordered position.
 *
 * <p>Callers must provide explicit ordering in {@code QueryCriteria}. The ordering should end in
 * a stable unique field to prevent duplicate or skipped rows when records change between pages.</p>
 *
 * @author RollW
 */
public final class KeysetWindow implements QueryWindow {
    private final int limit;
    private final @Nullable KeysetPosition after;

    /**
     * Creates the first keyset page.
     *
     * @param limit the requested positive page limit
     */
    public KeysetWindow(int limit) {
        this(limit, null);
    }

    /**
     * Creates a keyset page after a validated position.
     *
     * @param limit the requested positive page limit
     * @param after the optional position returned by the previous page
     */
    public KeysetWindow(int limit, @Nullable KeysetPosition after) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }
        this.limit = limit;
        this.after = after;
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
     * Returns the optional previous page position.
     *
     * @return the continuation position, when present
     */
    public @Nullable KeysetPosition after() {
        return after;
    }
}
