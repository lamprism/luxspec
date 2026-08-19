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

import com.lamprism.luxspec.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.util.Objects;

/**
 * Provides conservative default HTTP statuses for common foundation errors.
 *
 * @author RollW
 */
public class DefaultErrorHttpStatusResolver implements ErrorHttpStatusResolver {
    @Override
    public HttpStatusCode resolve(ErrorCode errorCode) {
        ErrorCode nonNullErrorCode = Objects.requireNonNull(errorCode, "errorCode");
        return switch (nonNullErrorCode.getCode()) {
            case "common:invalid-argument",
                 "resource:invalid-reference",
                 "security:unsupported-resource-action" -> HttpStatus.BAD_REQUEST;
            case "common:not-found", "resource:not-found" -> HttpStatus.NOT_FOUND;
            case "security:permission-denied", "security:resource-access-denied" -> HttpStatus.FORBIDDEN;
            case "security:invalid-token",
                 "security:access-token-revoked",
                 "security:refresh-token-rejected",
                 "security:unsupported-credentials",
                 "security:invalid-credentials",
                 "security:subject-not-found",
                 "security:subject-disabled",
                 "security:subject-locked",
                 "security:subject-canceled" -> HttpStatus.UNAUTHORIZED;
            case "common:unsupported-operation" -> HttpStatus.NOT_IMPLEMENTED;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
