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

import com.lamprism.luxspec.CommonErrorCode;
import com.lamprism.luxspec.LuxspecException;
import com.lamprism.luxspec.message.MessageResolver;
import com.lamprism.luxspec.web.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LuxspecExceptionHandlerTest {
    @Test
    void mapsExceptionsToSafeEnvelopesWithoutExposingInternalMessages() {
        LuxspecExceptionHandler handler = new LuxspecExceptionHandler(
                new DefaultErrorHttpStatusResolver()
        );
        LuxspecException exception = new LuxspecException(
                CommonErrorCode.INVALID_ARGUMENT,
                "database password and SQL details"
        );

        ResponseEntity<HttpResponse<Void>> response = handler.handle(exception, Locale.US);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        HttpResponse<Void> body = response.getBody();
        assertEquals("common:invalid-argument", body.status().code());
        assertNull(body.status().message());
        assertNull(body.data());
        assertNull(body.correlationId());
    }

    @Test
    void resolvesOnlyTheStableErrorCodeThroughTheConfiguredMessageResolver() {
        MessageResolver messageResolver = (key, locale, arguments) -> {
            assertEquals("common:invalid-argument", key);
            assertEquals(Locale.US, locale);
            assertEquals(0, arguments.length);
            return "The request is invalid.";
        };
        LuxspecExceptionHandler handler = new LuxspecExceptionHandler(
                new DefaultErrorHttpStatusResolver(),
                messageResolver
        );

        ResponseEntity<HttpResponse<Void>> response = handler.handle(
                new LuxspecException(CommonErrorCode.INVALID_ARGUMENT, "internal details"),
                Locale.US
        );

        assertEquals("The request is invalid.", response.getBody().status().message());
    }
}
