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

import java.util.Locale;
import java.util.Objects;

/**
 * Immutable request facts that are safe to expose through an execution context.
 *
 * @author RollW
 */
public final class WebRequestContext {
    private final String method;
    private final String path;
    private final Locale locale;

    /**
     * Creates request facts without retaining the underlying servlet request.
     *
     * @param method the HTTP method
     * @param path   the request path
     * @param locale the request locale
     */
    public WebRequestContext(String method, String path, Locale locale) {
        this.method = requireText(method, "method");
        this.path = requireText(path, "path");
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    /**
     * Returns the HTTP method.
     *
     * @return the HTTP method
     */
    public String method() {
        return method;
    }

    /**
     * Returns the request path.
     *
     * @return the request path
     */
    public String path() {
        return path;
    }

    /**
     * Returns the request locale selected by the framework boundary.
     *
     * @return the request locale
     */
    public Locale locale() {
        return locale;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WebRequestContext that)) {
            return false;
        }
        return method.equals(that.method)
                && path.equals(that.path)
                && locale.equals(that.locale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, path, locale);
    }

    private static String requireText(String value, String name) {
        String nonNullValue = Objects.requireNonNull(value, name);
        if (nonNullValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return nonNullValue;
    }
}
