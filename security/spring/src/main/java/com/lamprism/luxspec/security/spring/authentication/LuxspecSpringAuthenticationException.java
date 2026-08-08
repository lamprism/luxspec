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

package com.lamprism.luxspec.security.spring.authentication;

import com.lamprism.luxspec.ErrorCode;
import com.lamprism.luxspec.ErrorCodeCarrier;

import java.util.Objects;

/**
 * Adapts a trusted Luxspec authentication failure for Spring Security entry points.
 *
 * @author RollW
 */
public final class LuxspecSpringAuthenticationException
        extends org.springframework.security.core.AuthenticationException
        implements ErrorCodeCarrier {
    /**
     * The stable Luxspec authentication error preserved for adapter consumers.
     */
    private final ErrorCode errorCode;

    /**
     * Creates a generic Spring Security failure while preserving the stable Luxspec error code.
     *
     * @param cause the trusted Luxspec authentication failure
     */
    public LuxspecSpringAuthenticationException(com.lamprism.luxspec.security.authentication.AuthenticationException cause) {
        super("Access token was rejected", Objects.requireNonNull(cause, "cause"));
        this.errorCode = cause.getErrorCode();
    }

    /**
     * Returns the stable Luxspec authentication error code.
     *
     * @return the error code
     */
    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
