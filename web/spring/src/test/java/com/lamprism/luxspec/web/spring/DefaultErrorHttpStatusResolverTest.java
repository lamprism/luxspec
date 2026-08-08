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

import com.lamprism.luxspec.AuthErrorCode;
import com.lamprism.luxspec.CommonErrorCode;
import com.lamprism.luxspec.ErrorCodes;
import com.lamprism.luxspec.resource.ResourceErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultErrorHttpStatusResolverTest {
    private final DefaultErrorHttpStatusResolver resolver = new DefaultErrorHttpStatusResolver();

    @Test
    void mapsCommonAndResourceClientErrors() {
        assertEquals(HttpStatus.BAD_REQUEST, resolver.resolve(CommonErrorCode.INVALID_ARGUMENT));
        assertEquals(HttpStatus.NOT_FOUND, resolver.resolve(CommonErrorCode.NOT_FOUND));
        assertEquals(HttpStatus.BAD_REQUEST, resolver.resolve(ResourceErrorCode.INVALID_REFERENCE));
        assertEquals(HttpStatus.NOT_FOUND, resolver.resolve(ResourceErrorCode.NOT_FOUND));
    }

    @Test
    void mapsAuthenticationAndAuthorizationFailures() {
        assertEquals(HttpStatus.UNAUTHORIZED, resolver.resolve(AuthErrorCode.INVALID_TOKEN));
        assertEquals(HttpStatus.FORBIDDEN, resolver.resolve(AuthErrorCode.PERMISSION_DENIED));
        assertEquals(
                HttpStatus.BAD_REQUEST,
                resolver.resolve(ErrorCodes.of("security:unsupported-resource-action"))
        );
    }

    @Test
    void hidesUnknownFailuresBehindAnInternalServerError() {
        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                resolver.resolve(ErrorCodes.of("application:internal-failure"))
        );
    }
}
