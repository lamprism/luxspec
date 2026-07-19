package com.lamprism.luxspec.web;

/**
 * Contains no-count slice pagination metadata.
 *
 * @author RollW
 */
public final class SlicePagination implements CollectionPagination {
    private final long offset;
    private final int limit;
    private final boolean hasNext;

    public SlicePagination(long offset, int limit, boolean hasNext) {
        if (offset < 0L || limit < 1) {
            throw new IllegalArgumentException("Invalid slice pagination metadata");
        }
        this.offset = offset;
        this.limit = limit;
        this.hasNext = hasNext;
    }

    public long offset() {
        return offset;
    }
    public int limit() {
        return limit;
    }
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public String getMode() {
        return "slice";
    }
}
