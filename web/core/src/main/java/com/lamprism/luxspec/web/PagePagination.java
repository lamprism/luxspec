package com.lamprism.luxspec.web;

/**
 * Contains exact-total pagination metadata.
 *
 * @author RollW
 */
public final class PagePagination implements CollectionPagination {
    private final long offset;
    private final int limit;
    private final long total;

    public PagePagination(long offset, int limit, long total) {
        if (offset < 0L || limit < 1 || total < offset) {
            throw new IllegalArgumentException("Invalid page pagination metadata");
        }
        this.offset = offset;
        this.limit = limit;
        this.total = total;
    }

    public long offset() {
        return offset;
    }
    public int limit() {
        return limit;
    }
    public long total() {
        return total;
    }

    @Override
    public String getMode() {
        return "page";
    }
}
