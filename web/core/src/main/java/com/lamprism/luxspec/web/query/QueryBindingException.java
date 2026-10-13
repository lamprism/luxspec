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

package com.lamprism.luxspec.web.query;


import java.util.Objects;

/**
 * Reports a safe, client-correctable HTTP query binding failure.
 *
 * @author RollW
 */
public final class QueryBindingException extends RuntimeException {
    /**
     * The name of the invalid query parameter.
     */
    private final String parameter;

    /**
     * Creates a query binding failure.
     *
     * @param parameter the invalid parameter name
     * @param message   the safe failure message
     */
    public QueryBindingException(String parameter, String message) {
        super(Objects.requireNonNull(message, "message"));
        this.parameter = requireParameter(parameter);
    }

    /**
     * Returns the invalid parameter name.
     *
     * @return the parameter name
     */
    public String getParameter() {
        return parameter;
    }

    private static String requireParameter(String parameter) {
        String nonNullParameter = Objects.requireNonNull(parameter, "parameter");
        if (nonNullParameter.isBlank()) {
            throw new IllegalArgumentException("parameter must not be blank");
        }
        return nonNullParameter;
    }
}
