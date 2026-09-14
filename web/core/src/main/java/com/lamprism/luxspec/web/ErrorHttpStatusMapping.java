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

import com.lamprism.luxspec.ErrorCode;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Partially maps business errors to HTTP status codes.
 *
 * <p>A mapping returns an empty result when it does not own an error. Mapping chains use the first
 * non-empty result, allowing applications to add or override individual mappings without replacing
 * the complete HTTP error policy.</p>
 *
 * <p>Implementations may be invoked concurrently and must be thread-safe and free of externally
 * visible side effects.</p>
 *
 * @author RollW
 */
@FunctionalInterface
public interface ErrorHttpStatusMapping {
    /**
     * Attempts to map one stable business error.
     *
     * @param errorCode the error to map
     * @return the HTTP status, or empty when this mapping does not own the error
     */
    Optional<HttpStatusCode> map(ErrorCode errorCode);

    /**
     * Creates an immutable exact-identity mapping for one or more errors.
     *
     * <p>Matching uses canonical error-code values so equivalent {@link ErrorCode}
     * implementations are handled consistently.</p>
     *
     * @param statusCode the status returned for matching errors
     * @param errorCodes the errors to match
     * @return a thread-safe exact-identity mapping
     * @throws IllegalArgumentException if no error code is supplied
     */
    static ErrorHttpStatusMapping forErrors(HttpStatusCode statusCode, ErrorCode... errorCodes) {
        HttpStatusCode nonNullStatusCode = Objects.requireNonNull(statusCode, "statusCode");
        ErrorCode[] nonNullErrorCodes = Objects.requireNonNull(errorCodes, "errorCodes");
        if (nonNullErrorCodes.length == 0) {
            throw new IllegalArgumentException("errorCodes must not be empty");
        }

        Set<String> identities = new LinkedHashSet<>();
        for (ErrorCode errorCode : nonNullErrorCodes) {
            identities.add(Objects.requireNonNull(errorCode, "errorCode").getCode());
        }
        Set<String> immutableIdentities = Set.copyOf(identities);
        return errorCode -> {
            String identity = Objects.requireNonNull(errorCode, "errorCode").getCode();
            if (!immutableIdentities.contains(identity)) {
                return Optional.empty();
            }
            return Optional.of(nonNullStatusCode);
        };
    }
}
