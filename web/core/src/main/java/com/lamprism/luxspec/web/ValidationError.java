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
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Describes one safe structured validation failure.
 *
 * @author RollW
 */
public final class ValidationError {
    private final String field;
    private final ErrorCode code;
    private final String message;

    /**
     * Creates one structured validation failure.
     *
     * @param field   the invalid field name
     * @param code    the stable validation error code
     * @param message the optional safe validation message
     */
    public ValidationError(String field, ErrorCode code, @Nullable String message) {
        Objects.requireNonNull(field, "field");
        if (field.isBlank()) {
            throw new IllegalArgumentException("field must not be blank");
        }
        this.field = field;
        this.code = Objects.requireNonNull(code, "code");
        this.message = message;
    }

    /**
     * Returns the invalid field name.
     *
     * @return the field name
     */
    public String field() {
        return field;
    }

    /**
     * Returns the stable validation error code.
     *
     * @return the validation error code
     */
    public ErrorCode code() {
        return code;
    }

    /**
     * Returns the optional safe validation message.
     *
     * @return the message, when present
     */
    public @Nullable String message() {
        return message;
    }
}
