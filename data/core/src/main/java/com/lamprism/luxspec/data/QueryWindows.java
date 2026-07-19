package com.lamprism.luxspec.data;

final class QueryWindows {
    private QueryWindows() {
    }

    static void requireRange(long offset, int limit) {
        if (offset < 0L) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }
    }
}
