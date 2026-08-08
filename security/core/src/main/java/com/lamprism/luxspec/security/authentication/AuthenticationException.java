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

package com.lamprism.luxspec.security.authentication;

import com.lamprism.luxspec.ErrorCode;
import com.lamprism.luxspec.LuxspecException;
import org.jspecify.annotations.Nullable;

/**
 * Indicates a trusted authentication failure with a stable internal code.
 *
 * @author RollW
 */
public class AuthenticationException extends LuxspecException {
    /**
     * Creates an authentication failure with no underlying cause.
     *
     * @param errorCode the stable authentication error
     * @param message   the trusted-backend failure message
     */
    public AuthenticationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Creates an authentication failure with an optional internal cause.
     *
     * @param errorCode the stable authentication error
     * @param message   the trusted-backend failure message
     * @param cause     the optional internal cause
     */
    public AuthenticationException(ErrorCode errorCode, String message, @Nullable Throwable cause) {
        super(errorCode, message, cause);
    }
}
