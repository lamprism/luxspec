package com.lamprism.luxspec.data;

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
