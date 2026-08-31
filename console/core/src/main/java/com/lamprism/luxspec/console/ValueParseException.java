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

package com.lamprism.luxspec.console;

import java.util.Objects;

/**
 * Signals that one command token cannot be converted to the requested type.
 *
 * @author RollW
 */
public final class ValueParseException extends Exception {
    /**
     * Token that could not be converted.
     */
    private final String token;

    /**
     * Creates a value conversion failure.
     *
     * @param token   token that could not be converted
     * @param message failure message
     */
    public ValueParseException(String token, String message) {
        super(Objects.requireNonNull(message, "message"));
        this.token = Objects.requireNonNull(token, "token");
    }

    /**
     * Creates a value conversion failure with an underlying cause.
     *
     * @param token   token that could not be converted
     * @param message failure message
     * @param cause   underlying conversion cause
     */
    public ValueParseException(String token, String message, Throwable cause) {
        super(Objects.requireNonNull(message, "message"), Objects.requireNonNull(cause, "cause"));
        this.token = Objects.requireNonNull(token, "token");
    }

    /**
     * @return the token that failed conversion
     */
    public String getToken() {
        return token;
    }
}
