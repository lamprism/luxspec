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

import com.lamprism.luxspec.ErrorCode;

/**
 * Maps a provider-independent business error to an HTTP response status.
 *
 * <p>Applications may contribute individual {@link ErrorHttpStatusMapping} instances to the
 * default resolver, or replace this strategy when their HTTP contract requires completely
 * different status semantics.</p>
 *
 * <p>Implementations may be invoked concurrently and must be thread-safe.</p>
 *
 * @author RollW
 */
@FunctionalInterface
public interface ErrorHttpStatusResolver {
    /**
     * Resolves the HTTP status for one stable business error.
     *
     * @param errorCode the error to map
     * @return the HTTP status code
     */
    HttpStatusCode resolve(ErrorCode errorCode);
}
