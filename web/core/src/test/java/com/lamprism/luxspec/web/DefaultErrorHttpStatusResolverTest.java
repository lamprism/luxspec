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

import com.lamprism.luxspec.AuthErrorCode;
import com.lamprism.luxspec.CommonErrorCode;
import com.lamprism.luxspec.ErrorCode;
import com.lamprism.luxspec.resource.ResourceErrorCode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultErrorHttpStatusResolverTest {
    private static final HttpStatusCode CONFLICT = new HttpStatusCode(409);
    private static final HttpStatusCode GONE = new HttpStatusCode(410);

    private final DefaultErrorHttpStatusResolver resolver =
            new DefaultErrorHttpStatusResolver(List.of());

    @Test
    void mapsCommonAndResourceClientErrors() {
        assertEquals(
                HttpStatusCode.BAD_REQUEST,
                resolver.resolve(CommonErrorCode.INVALID_ARGUMENT)
        );
        assertEquals(HttpStatusCode.NOT_FOUND, resolver.resolve(CommonErrorCode.NOT_FOUND));
        assertEquals(
                HttpStatusCode.BAD_REQUEST,
                resolver.resolve(ResourceErrorCode.INVALID_REFERENCE)
        );
        assertEquals(HttpStatusCode.NOT_FOUND, resolver.resolve(ResourceErrorCode.NOT_FOUND));
    }

    @Test
    void mapsAuthenticationAndAuthorizationFailures() {
        assertEquals(
                HttpStatusCode.UNAUTHORIZED,
                resolver.resolve(AuthErrorCode.INVALID_TOKEN)
        );
        assertEquals(HttpStatusCode.FORBIDDEN, resolver.resolve(AuthErrorCode.PERMISSION_DENIED));
    }

    @Test
    void mapsEquivalentErrorCodeImplementationsByCanonicalIdentity() {
        assertEquals(
                HttpStatusCode.BAD_REQUEST,
                resolver.resolve(ErrorCode.of(CommonErrorCode.INVALID_ARGUMENT.getCode()))
        );
    }

    @Test
    void hidesUnknownFailuresBehindAnInternalServerError() {
        assertEquals(
                HttpStatusCode.INTERNAL_SERVER_ERROR,
                resolver.resolve(ErrorCode.of("application:internal-failure"))
        );
    }

    @Test
    void addsAnApplicationMappingWithoutReplacingFoundationDefaults() {
        DefaultErrorHttpStatusResolver extendedResolver = new DefaultErrorHttpStatusResolver(
                List.of(ErrorHttpStatusMapping.forErrors(
                        CONFLICT,
                        ErrorCode.of("application:conflict")
                ))
        );

        assertEquals(
                CONFLICT,
                extendedResolver.resolve(ErrorCode.of("application:conflict"))
        );
        assertEquals(
                HttpStatusCode.BAD_REQUEST,
                extendedResolver.resolve(CommonErrorCode.INVALID_ARGUMENT)
        );
    }

    @Test
    void applicationMappingsOverrideFoundationDefaults() {
        DefaultErrorHttpStatusResolver overridingResolver = new DefaultErrorHttpStatusResolver(
                List.of(ErrorHttpStatusMapping.forErrors(
                        GONE,
                        CommonErrorCode.NOT_FOUND
                ))
        );

        assertEquals(GONE, overridingResolver.resolve(CommonErrorCode.NOT_FOUND));
    }

    @Test
    void usesTheFirstApplicationMappingThatReturnsAStatus() {
        ErrorHttpStatusMapping first = errorCode -> Optional.of(CONFLICT);
        ErrorHttpStatusMapping second = errorCode -> Optional.of(GONE);
        DefaultErrorHttpStatusResolver orderedResolver = new DefaultErrorHttpStatusResolver(
                List.of(first, second)
        );

        assertEquals(
                CONFLICT,
                orderedResolver.resolve(ErrorCode.of("application:failure"))
        );
    }

    @Test
    void copiesApplicationMappingsAtConstruction() {
        List<ErrorHttpStatusMapping> mappings = new ArrayList<>();
        DefaultErrorHttpStatusResolver snapshotResolver =
                new DefaultErrorHttpStatusResolver(mappings);
        mappings.add(errorCode -> Optional.of(GONE));

        assertEquals(
                HttpStatusCode.BAD_REQUEST,
                snapshotResolver.resolve(CommonErrorCode.INVALID_ARGUMENT)
        );
    }

    @Test
    void rejectsAnEmptyExactIdentityMapping() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ErrorHttpStatusMapping.forErrors(HttpStatusCode.BAD_REQUEST)
        );
    }
}
