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

package com.lamprism.luxspec.security.spring;

import com.lamprism.luxspec.AuthErrorCode;
import com.lamprism.luxspec.web.DefaultErrorHttpStatusResolver;
import com.lamprism.luxspec.web.ErrorHttpStatusResolver;
import com.lamprism.luxspec.web.HttpResponse;
import com.lamprism.luxspec.web.ResponseStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Adapts controller and method-security access denials to the standard Luxspec HTTP envelope.
 *
 * <p>Security-filter denials happen before MVC exception resolution and remain owned by the
 * application's configured {@code AccessDeniedHandler}. This advice covers denials raised while
 * invoking an MVC controller or secured method.</p>
 *
 * @author RollW
 */
@RestControllerAdvice
public class LuxspecSecurityExceptionHandler {
    private final ErrorHttpStatusResolver statusResolver;

    /**
     * Creates an adapter using the foundation error-to-status mapping.
     */
    public LuxspecSecurityExceptionHandler() {
        this(new DefaultErrorHttpStatusResolver(List.of()));
    }

    /**
     * Creates an adapter with an application-owned error-to-status mapping.
     *
     * @param statusResolver the error-to-status mapping
     */
    public LuxspecSecurityExceptionHandler(ErrorHttpStatusResolver statusResolver) {
        this.statusResolver = Objects.requireNonNull(statusResolver, "statusResolver");
    }

    /**
     * Converts an access denial without exposing framework exception details.
     *
     * @param exception the access denial
     * @param locale    the request locale
     * @return the forbidden response envelope
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<HttpResponse<Void>> handle(AccessDeniedException exception, Locale locale) {
        Objects.requireNonNull(exception, "exception");
        Objects.requireNonNull(locale, "locale");
        ResponseStatus status = ResponseStatus.failure(AuthErrorCode.PERMISSION_DENIED, null);
        return ResponseEntity.status(statusResolver.resolve(AuthErrorCode.PERMISSION_DENIED).value())
                .body(HttpResponse.failure(status, null, null));
    }
}
