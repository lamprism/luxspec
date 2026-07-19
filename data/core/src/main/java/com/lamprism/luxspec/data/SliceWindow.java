package com.lamprism.luxspec.data;

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
     * @param limit the required positive slice limit
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
