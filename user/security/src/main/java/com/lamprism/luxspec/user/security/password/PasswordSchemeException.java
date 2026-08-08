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

package com.lamprism.luxspec.user.security.password;

import com.lamprism.luxspec.LuxspecException;
import org.jspecify.annotations.Nullable;

/**
 * Indicates that a protected password representation could not be processed safely.
 *
 * @author RollW
 */
public final class PasswordSchemeException extends LuxspecException {
    /**
     * Creates a password-scheme failure without an underlying cause.
     *
     * @param errorCode the stable password-scheme error
     * @param message   the trusted-backend failure message
     */
    public PasswordSchemeException(PasswordErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Creates a password-scheme failure with an optional underlying cause.
     *
     * @param errorCode the stable password-scheme error
     * @param message   the trusted-backend failure message
     * @param cause     the optional internal cause
     */
    public PasswordSchemeException(PasswordErrorCode errorCode, String message, @Nullable Throwable cause) {
        super(errorCode, message, cause);
    }
}
