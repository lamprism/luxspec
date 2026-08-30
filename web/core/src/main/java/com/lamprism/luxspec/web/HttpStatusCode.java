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

/**
 * Provider-independent HTTP response status code.
 *
 * <p>The value remains open to registered and application-specific HTTP status codes while
 * enforcing the three-digit HTTP status range.</p>
 *
 * @param value the numeric HTTP status code
 * @author RollW
 */
public record HttpStatusCode(int value) {
    /**
     * HTTP 400 Bad Request.
     */
    public static final HttpStatusCode BAD_REQUEST = new HttpStatusCode(400);

    /**
     * HTTP 401 Unauthorized.
     */
    public static final HttpStatusCode UNAUTHORIZED = new HttpStatusCode(401);

    /**
     * HTTP 403 Forbidden.
     */
    public static final HttpStatusCode FORBIDDEN = new HttpStatusCode(403);

    /**
     * HTTP 404 Not Found.
     */
    public static final HttpStatusCode NOT_FOUND = new HttpStatusCode(404);

    /**
     * HTTP 500 Internal Server Error.
     */
    public static final HttpStatusCode INTERNAL_SERVER_ERROR = new HttpStatusCode(500);

    /**
     * HTTP 501 Not Implemented.
     */
    public static final HttpStatusCode NOT_IMPLEMENTED = new HttpStatusCode(501);

    /**
     * Creates an HTTP status code.
     *
     * @throws IllegalArgumentException if the value is outside the range from 100 through 599
     */
    public HttpStatusCode {
        if (value < 100 || value > 599) {
            throw new IllegalArgumentException("HTTP status code must be between 100 and 599");
        }
    }
}
