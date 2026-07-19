package com.lamprism.luxspec.web;


/**
 * Indicates that a collection is complete.
 *
 * @author RollW
 */
public final class CompletePagination implements CollectionPagination {
    private static final CompletePagination INSTANCE = new CompletePagination();

    private CompletePagination() {
    }

    public static CompletePagination getInstance() {
        return INSTANCE;
    }

    @Override
    public String getMode() {
        return "complete";
    }
}
