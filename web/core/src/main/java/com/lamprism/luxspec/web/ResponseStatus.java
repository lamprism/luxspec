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
 * Describes the stable result code and optional safe message of an HTTP response.
 *
 * @author RollW
 */
public final class ResponseStatus {
    private static final ResponseStatus SUCCESS = new ResponseStatus("success", null);

    private final String code;

    @Nullable
    private final String message;

    private ResponseStatus(String code, @Nullable String message) {
        this.code = Objects.requireNonNull(code, "code");
        this.message = message;
    }

    /**
     * Creates a failed status from a stable business error.
     *
     * @param errorCode the stable business error
     * @param message   the optional safe response message
     * @return the failed response status
     */
    public static ResponseStatus failure(ErrorCode errorCode, @Nullable String message) {
        Objects.requireNonNull(errorCode, "errorCode");
        return new ResponseStatus(errorCode.getCode(), message);
    }

    /**
     * Returns the JSON-visible stable result code.
     *
     * @return the stable result code
     */
    public String code() {
        return code;
    }

    /**
     * Returns the optional safe response message.
     *
     * @return the response message, when present
     */
    @Nullable
    public String message() {
        return message;
    }

    /**
     * Returns the shared successful status.
     *
     * @return the successful response status
     */
    public static ResponseStatus success() {
        return SUCCESS;
    }
}
