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

package com.lamprism.luxspec.web.spring;

import com.lamprism.luxspec.ErrorCode;
import org.springframework.http.HttpStatusCode;

/**
 * Maps a provider-independent business error code to an HTTP response status.
 *
 * @author RollW
 */
public interface ErrorHttpStatusResolver {
    /**
     * Resolves the HTTP status for one stable business error.
     *
     * @param errorCode the error to map
     * @return the HTTP status code
     */
    HttpStatusCode resolve(ErrorCode errorCode);
}
