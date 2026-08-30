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

package com.lamprism.luxspec.web.spring;

import com.lamprism.luxspec.LuxspecException;
import com.lamprism.luxspec.message.MessageResolver;
import com.lamprism.luxspec.web.ErrorHttpStatusResolver;
import com.lamprism.luxspec.web.HttpResponse;
import com.lamprism.luxspec.web.HttpStatusCode;
import com.lamprism.luxspec.web.ResponseStatus;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;
import java.util.Objects;

/**
 * Converts error-code-carrying exceptions into ordinary Luxspec JSON error envelopes.
 *
 * <p>This advice maps a domain failure into an HTTP result. It is intentionally not a
 * {@link com.lamprism.luxspec.failure.FailureHandler}: that callback observes a failure and has no
 * result, while this boundary must return a protocol response without exposing internal details.</p>
 *
 * @author RollW
 */
@RestControllerAdvice
public class LuxspecExceptionHandler {
    private final ErrorHttpStatusResolver statusResolver;
    private final @Nullable MessageResolver messageResolver;

    /**
     * Creates an exception advice with an explicit business-error HTTP mapping policy.
     *
     * @param statusResolver the business-error HTTP mapping policy
     */
    public LuxspecExceptionHandler(ErrorHttpStatusResolver statusResolver) {
        this(statusResolver, null);
    }

    /**
     * Creates an exception advice with optional safe message localization.
     *
     * @param statusResolver  the business-error HTTP mapping policy
     * @param messageResolver the optional message resolver
     */
    public LuxspecExceptionHandler(
            ErrorHttpStatusResolver statusResolver,
            @Nullable MessageResolver messageResolver
    ) {
        this.statusResolver = Objects.requireNonNull(statusResolver, "statusResolver");
        this.messageResolver = messageResolver;
    }

    /**
     * Maps a stable error-code-carrying exception without exposing internal exception details.
     *
     * @param exception the error-code-carrying exception
     * @param locale    the request locale
     * @return the HTTP response entity
     */
    @ExceptionHandler(LuxspecException.class)
    public ResponseEntity<HttpResponse<Void>> handle(LuxspecException exception, Locale locale) {
        LuxspecException nonNullException = Objects.requireNonNull(exception, "exception");
        Locale nonNullLocale = Objects.requireNonNull(locale, "locale");
        ResponseStatus status = ResponseStatus.failure(
                nonNullException.getErrorCode(),
                resolveMessage(nonNullException, nonNullLocale)
        );
        HttpStatusCode httpStatusCode = statusResolver.resolve(nonNullException.getErrorCode());
        return ResponseEntity.status(httpStatusCode.value())
                .body(HttpResponse.failure(status, null, null));
    }

    private @Nullable String resolveMessage(LuxspecException exception, Locale locale) {
        if (messageResolver == null) {
            return null;
        }
        return messageResolver.resolve(exception.getErrorCode().getCode(), locale);
    }
}
