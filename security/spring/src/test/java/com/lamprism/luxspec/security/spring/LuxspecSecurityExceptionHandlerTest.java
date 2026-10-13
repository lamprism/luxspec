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
import com.lamprism.luxspec.web.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LuxspecSecurityExceptionHandlerTest {
    @Test
    void mapsControllerAccessDenialToTheStandardForbiddenEnvelope() {
        LuxspecSecurityExceptionHandler handler = new LuxspecSecurityExceptionHandler();

        ResponseEntity<HttpResponse<Void>> response = handler.handle(
                new AccessDeniedException("internal security detail"),
                Locale.US
        );

        assertEquals(403, response.getStatusCode().value());
        HttpResponse<Void> body = response.getBody();
        assertEquals(AuthErrorCode.PERMISSION_DENIED.getCode(), body.status().code());
        assertNull(body.status().message());
        assertNull(body.data());
    }
}
