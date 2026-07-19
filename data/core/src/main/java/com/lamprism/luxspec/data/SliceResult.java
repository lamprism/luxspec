package com.lamprism.luxspec.data;

import java.util.List;

/**
 * Represents an offset slice without an exact total count.
 *
 * @param <T> the element type
 * @author RollW
 */
public final class SliceResult<T> implements QueryResult<T> {
    private final List<T> items;
    private final long offset;
    private final int limit;
    private final boolean hasNext;

    /**
     * Creates a slice result without an exact total count.
     *
     * @param items the slice items
     * @param offset the zero-based result offset
     * @param limit the requested slice limit
     * @param hasNext whether another slice may be requested
     */
    public SliceResult(List<T> items, long offset, int limit, boolean hasNext) {
        QueryWindows.requireRange(offset, limit);
        List<T> copiedItems = List.copyOf(items);
        if (copiedItems.size() > limit) {
            throw new IllegalArgumentException("slice items must not exceed the requested limit");
        }
        this.items = copiedItems;
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
     * Returns the requested slice limit.
     *
     * @return the limit
     */
    public int limit() {
        return limit;
    }

    /**
     * Reports whether another slice may contain items.
     *
     * @return true when another slice may be requested
     */
    public boolean hasNext() {
        return hasNext;
    }

    /**
     * Returns immutable slice items.
     *
     * @return the slice items
     */
    @Override
    public List<T> getItems() {
        return items;
    }
}
