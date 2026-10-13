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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Stores the ordered field values at one keyset page boundary.
 *
 * <p>The values are intentionally not decoded from an untrusted token by this type. A Web or
 * application boundary must validate and decode a cursor before constructing this position.</p>
 *
 * @author RollW
 */
public final class KeysetPosition {
    private final List<Object> values;

    /**
     * Creates an immutable keyset position.
     *
     * @param values the non-empty ordered field values
     */
    public KeysetPosition(List<?> values) {
        List<?> nonNullValues = Objects.requireNonNull(values, "values");
        if (nonNullValues.isEmpty()) {
            throw new IllegalArgumentException("Keyset position must contain at least one value");
        }
        List<Object> copiedValues = new ArrayList<>(nonNullValues.size());
        for (Object value : nonNullValues) {
            copiedValues.add(Objects.requireNonNull(value, "keyset value"));
        }
        this.values = List.copyOf(copiedValues);
    }

    /**
     * Returns the immutable values in the same order as the query's order terms.
     *
     * @return the keyset values
     */
    public List<Object> values() {
        return values;
    }
}
