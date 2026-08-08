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

import com.lamprism.luxspec.context.ContextKey;

/**
 * Typed execution-context keys owned by the Web domain.
 *
 * @author RollW
 */
public final class WebContextKeys {
    /**
     * Identifies the immutable request facts for the current Web request.
     */
    public static final ContextKey<WebRequestContext> REQUEST = ContextKey.of(
            "web.request",
            WebRequestContext.class
    );

    /**
     * Identifies the safe trace identifier for the current Web request.
     */
    public static final ContextKey<TraceContext> TRACE = ContextKey.of(
            "web.trace",
            TraceContext.class
    );

    private WebContextKeys() {
    }
}
