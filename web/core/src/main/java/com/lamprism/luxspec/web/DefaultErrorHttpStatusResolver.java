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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves application mappings before conservative foundation HTTP mappings.
 *
 * <p>Application mappings are evaluated in iteration order and the first result wins. Foundation
 * mappings run afterward, followed by an internal-server-error fallback. This allows an application
 * to add a mapping or override one default without reproducing the complete policy.</p>
 *
 * <p>The supplied mappings are copied during construction. Mapping implementations may be invoked
 * concurrently and must satisfy the {@link ErrorHttpStatusMapping} concurrency contract.</p>
 *
 * @author RollW
 */
public final class DefaultErrorHttpStatusResolver implements ErrorHttpStatusResolver {
    private static final List<ErrorHttpStatusMapping> FOUNDATION_MAPPINGS = List.of(
            ErrorHttpStatusMapping.forErrors(
                    HttpStatusCode.BAD_REQUEST,
                    CommonErrorCode.INVALID_ARGUMENT,
                    ResourceErrorCode.INVALID_REFERENCE
            ),
            ErrorHttpStatusMapping.forErrors(
                    HttpStatusCode.NOT_FOUND,
                    CommonErrorCode.NOT_FOUND,
                    ResourceErrorCode.NOT_FOUND
            ),
            ErrorHttpStatusMapping.forErrors(
                    HttpStatusCode.FORBIDDEN,
                    AuthErrorCode.PERMISSION_DENIED
            ),
            ErrorHttpStatusMapping.forErrors(
                    HttpStatusCode.UNAUTHORIZED,
                    AuthErrorCode.INVALID_TOKEN,
                    AuthErrorCode.ACCESS_TOKEN_REVOKED,
                    AuthErrorCode.REFRESH_TOKEN_REJECTED,
                    AuthErrorCode.UNSUPPORTED_CREDENTIALS,
                    AuthErrorCode.INVALID_CREDENTIALS,
                    AuthErrorCode.SUBJECT_NOT_FOUND,
                    AuthErrorCode.SUBJECT_DISABLED,
                    AuthErrorCode.SUBJECT_LOCKED,
                    AuthErrorCode.SUBJECT_CANCELED
            ),
            ErrorHttpStatusMapping.forErrors(
                    HttpStatusCode.NOT_IMPLEMENTED,
                    CommonErrorCode.UNSUPPORTED_OPERATION
            )
    );

    private final List<ErrorHttpStatusMapping> mappings;

    /**
     * Creates a resolver with ordered application mappings and foundation defaults.
     *
     * @param mappings application mappings evaluated before the foundation defaults
     */
    public DefaultErrorHttpStatusResolver(
            Iterable<? extends ErrorHttpStatusMapping> mappings
    ) {
        Objects.requireNonNull(mappings, "mappings");
        List<ErrorHttpStatusMapping> orderedMappings = new ArrayList<>();
        for (ErrorHttpStatusMapping mapping : mappings) {
            orderedMappings.add(Objects.requireNonNull(mapping, "mapping"));
        }
        orderedMappings.addAll(FOUNDATION_MAPPINGS);
        this.mappings = List.copyOf(orderedMappings);
    }

    @Override
    public HttpStatusCode resolve(ErrorCode errorCode) {
        ErrorCode nonNullErrorCode = Objects.requireNonNull(errorCode, "errorCode");
        for (ErrorHttpStatusMapping mapping : mappings) {
            Optional<HttpStatusCode> statusCode = Objects.requireNonNull(
                    mapping.map(nonNullErrorCode),
                    "mapping result"
            );
            if (statusCode.isPresent()) {
                return statusCode.orElseThrow();
            }
        }
        return HttpStatusCode.INTERNAL_SERVER_ERROR;
    }
}
