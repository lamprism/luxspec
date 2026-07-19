package com.lamprism.luxspec.web;

import com.lamprism.luxspec.data.CompleteResult;
import com.lamprism.luxspec.data.PageResult;
import com.lamprism.luxspec.data.QueryResult;
import com.lamprism.luxspec.data.SliceResult;
import java.util.List;
import java.util.Objects;

/**
 * Gives Web collection consumers one items field and explicit pagination metadata.
 *
 * @param <T> the item type
 * @author RollW
 */
public final class CollectionResponse<T> {
    private final List<T> items;
    private final CollectionPagination pagination;

    public CollectionResponse(List<T> items, CollectionPagination pagination) {
        this.items = List.copyOf(items);
        this.pagination = Objects.requireNonNull(pagination, "pagination");
    }

    public List<T> items() {
        return items;
    }

    public CollectionPagination pagination() {
        return pagination;
    }

    public static <T> CollectionResponse<T> from(List<T> items) {
        return new CollectionResponse<>(items, CompletePagination.getInstance());
    }

    public static <T> CollectionResponse<T> from(QueryResult<T> result) {
        Objects.requireNonNull(result, "result");
        if (result instanceof CompleteResult<?>) {
            return from(result.getItems());
        }
        if (result instanceof PageResult<?> pageResult) {
            return new CollectionResponse<>(
                    result.getItems(),
                    new PagePagination(pageResult.offset(), pageResult.limit(), pageResult.total())
            );
        }
        if (result instanceof SliceResult<?> sliceResult) {
            return new CollectionResponse<>(
                    result.getItems(),
                    new SlicePagination(sliceResult.offset(), sliceResult.limit(), sliceResult.hasNext())
            );
        }
        throw new IllegalArgumentException("Unsupported QueryResult type");
    }
}
