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
 * Creates validated error-code values for application-defined errors.
 *
 * @author RollW
 */
public final class ErrorCodes {
    private ErrorCodes() {
    }

    /**
     * Creates a canonical application-defined error-code value.
     *
     * <p>Values created by this factory use code-based equality with other values created by this
     * factory. Cross-implementation comparisons must use {@link ErrorCode#getCode()}.</p>
     *
     * @param code the lowercase colon-separated error code
     * @return the validated error code
     */
    public static ErrorCode of(String code) {
        Objects.requireNonNull(code, "code");
        requireCanonical(code);
        return new Value(code);
    }

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
            if ((character < 'a' || character > 'z') && character != '-' && (character < '0' || character > '9')) {
                throw new IllegalArgumentException("code contains an unsupported character");
            }
            segmentStart = false;
        }
        if (segmentStart) {
            throw new IllegalArgumentException("code must not end with a separator");
        }
    }

    private static final class Value implements ErrorCode {
        private final String code;

        private Value(String code) {
            this.code = code;
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Value value && code.equals(value.code);
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
}
