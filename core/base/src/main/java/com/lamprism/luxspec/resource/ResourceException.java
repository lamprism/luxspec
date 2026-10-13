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

package com.lamprism.luxspec.resource;

import org.jspecify.annotations.Nullable;

/**
 * Indicates a provider-independent resource failure.
 *
 * @author RollW
 */
public class ResourceException extends RuntimeException {
    /**
     * Creates a resource failure without an application-facing error code.
     *
     * @param message the safe failure description
     */
    public ResourceException(String message) {
        super(message);
    }

    /**
     * Creates a resource failure with an optional underlying cause.
     *
     * @param message the safe failure description
     * @param cause   the optional underlying failure
     */
    public ResourceException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
