package com.lamprism.luxspec.data;

import java.util.List;

/**
 * Represents a complete or windowed collection result.
 *
 * @param <T> the element type
 * @author RollW
 */
public sealed interface QueryResult<T> permits CompleteResult, PageResult, SliceResult {
    /**
     * Returns immutable result items in their query order.
     *
     * @return the result items
     */
    List<T> getItems();
}
