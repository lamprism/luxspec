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

package com.lamprism.luxspec;

import java.util.Objects;

/**
 * A stable provider-independent business error identifier.
 *
 * <p>Implementations may use different equality semantics. Callers comparing values from
 * different implementations must compare their canonical {@link #getCode()} values.</p>
 *
 * @author RollW
 */
public interface ErrorCode {
    /**
     * Creates a canonical application-defined error-code value.
     *
     * @param code the lowercase colon-separated error code
     * @return the validated error code
     */
    static ErrorCode of(String code) {
        String nonNullCode = Objects.requireNonNull(code, "code");
        requireCanonical(nonNullCode);
        return new DefaultErrorCode(nonNullCode);
    }

    /**
     * Returns the stable canonical business error identity.
     *
     * @return the error code
     */
    String getCode();

    private static void requireCanonical(String code) {
        if (code.isEmpty()) {
            throw new IllegalArgumentException("code must not be empty");
        }
        boolean segmentStart = true;
        for (int index = 0; index < code.length(); index++) {
            char character = code.charAt(index);
            if (character == ':') {
                if (segmentStart) {
                    throw new IllegalArgumentException("code contains an empty segment");
                }
                segmentStart = true;
                continue;
            }
            if ((character < 'a' || character > 'z') && character != '-'
                    && (character < '0' || character > '9')) {
                throw new IllegalArgumentException("code contains an unsupported character");
            }
            segmentStart = false;
        }
        if (segmentStart) {
            throw new IllegalArgumentException("code must not end with a separator");
        }
    }
}

final class DefaultErrorCode implements ErrorCode {
    private final String code;

    DefaultErrorCode(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DefaultErrorCode value && code.equals(value.code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }

    @Override
    public String toString() {
        return code;
    }
}
