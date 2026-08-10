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
 * Represents a complete collection result.
 *
 * @param <T> the element type
 * @author RollW
 */
public final class CompleteResult<T> implements QueryResult<T> {
    private final List<T> items;

    /**
     * Creates a complete immutable result.
     *
     * @param items the complete result items
     */
    public CompleteResult(List<T> items) {
        this.items = List.copyOf(items);
    }

    /**
     * Returns immutable complete result items.
     *
     * @return the result items
     */
    public List<T> items() {
        return items;
    }

    /**
     * Returns immutable complete result items.
     *
     * @return the result items
     */
    @Override
    public List<T> getItems() {
        return items;
    }
}
