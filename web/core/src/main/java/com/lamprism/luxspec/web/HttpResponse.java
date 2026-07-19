package com.lamprism.luxspec.web;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * The standard JSON response envelope for ordinary Luxspec HTTP APIs.
 *
 * @param <T> the response data type
 * @author RollW
 */
public final class HttpResponse<T> {
    private final ResponseStatus status;
    private final T data;
    private final String traceId;

    /**
     * Creates one ordinary JSON response envelope.
     */
    public HttpResponse(ResponseStatus status, @Nullable T data, @Nullable String traceId) {
        this.status = Objects.requireNonNull(status, "status");
        this.data = data;
        this.traceId = traceId;
    }

    /**
     * Returns the stable response status.
     */
    public ResponseStatus status() {
        return status;
    }

    /**
     * Returns optional response data.
     */
    public @Nullable T data() {
        return data;
    }

    /**
     * Returns the optional request trace identifier.
     */
    public @Nullable String traceId() {
        return traceId;
    }

    /**
     * Creates a successful response envelope.
     */
    public static <T> HttpResponse<T> success(@Nullable T data, @Nullable String traceId) {
        return new HttpResponse<>(ResponseStatus.success(), data, traceId);
    }

    /**
     * Creates a failed response envelope with an explicit status.
     */
    public static <T> HttpResponse<T> failure(
            ResponseStatus status,
            @Nullable T data,
            @Nullable String traceId
    ) {
        return new HttpResponse<>(status, data, traceId);
    }
}
