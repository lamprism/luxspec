package com.lamprism.luxspec.web;

/**
 * Identifies the pagination metadata variant of a collection response.
 *
 * @author RollW
 */
public sealed interface CollectionPagination permits CompletePagination, PagePagination, SlicePagination {
    String getMode();
}
