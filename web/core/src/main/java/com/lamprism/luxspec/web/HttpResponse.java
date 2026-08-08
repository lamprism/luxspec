/*
 * Copyright (C) Lamprism
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
     *
     * @param status  the response status
     * @param data    the optional response data
     * @param traceId the optional safe trace identifier
     */
    public HttpResponse(ResponseStatus status, @Nullable T data, @Nullable String traceId) {
        this.status = Objects.requireNonNull(status, "status");
        this.data = data;
        this.traceId = traceId;
    }

    /**
     * Returns the stable response status.
     *
     * @return the response status
     */
    public ResponseStatus status() {
        return status;
    }

    /**
     * Returns optional response data.
     *
     * @return the response data, when present
     */
    public @Nullable T data() {
        return data;
    }

    /**
     * Returns the optional request trace identifier.
     *
     * @return the trace identifier, when present
     */
    public @Nullable String traceId() {
        return traceId;
    }

    /**
     * Creates a successful response envelope.
     *
     * @param data    the optional response data
     * @param traceId the optional safe trace identifier
     * @param <T>     the response data type
     * @return the successful response
     */
    public static <T> HttpResponse<T> success(@Nullable T data, @Nullable String traceId) {
        return new HttpResponse<>(ResponseStatus.success(), data, traceId);
    }

    /**
     * Creates a failed response envelope with an explicit status.
     *
     * @param status  the response status
     * @param data    the optional response data
     * @param traceId the optional safe trace identifier
     * @param <T>     the response data type
     * @return the failed response
     */
    public static <T> HttpResponse<T> failure(
            ResponseStatus status,
            @Nullable T data,
            @Nullable String traceId
    ) {
        return new HttpResponse<>(status, data, traceId);
    }
}
