package com.lamprism.luxspec.data;

/**
 * Requests an offset range whose result must contain an exact total.
 *
 * @author RollW
 */
public final class PageWindow implements QueryWindow {
    private final long offset;
    private final int limit;

    /**
     * Creates a page window.
     *
     * @param offset the zero-based result offset
     * @param limit the required positive page limit
     */
    public PageWindow(long offset, int limit) {
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
     * Returns the requested page limit.
     *
     * @return the limit
     */
    public int limit() {
        return limit;
    }
}
