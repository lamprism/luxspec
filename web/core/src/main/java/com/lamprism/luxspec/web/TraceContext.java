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

import java.util.Objects;
import java.util.UUID;

/**
 * An immutable, response-safe correlation identifier for one request.
 *
 * @author RollW
 */
public final class TraceContext {
    private static final int MAX_LENGTH = 128;
    private final String traceId;

    private TraceContext(String traceId) {
        this.traceId = traceId;
    }

    /**
     * Creates a trace context from a validated external identifier.
     *
     * @param traceId the ASCII identifier to retain
     * @return the trace context
     */
    public static TraceContext of(String traceId) {
        return new TraceContext(validate(traceId));
    }

    /**
     * Creates a new random trace context.
     *
     * @return a generated trace context
     */
    public static TraceContext generated() {
        return of(UUID.randomUUID().toString());
    }

    /**
     * Returns the safe correlation identifier.
     *
     * @return the trace identifier
     */
    public String traceId() {
        return traceId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TraceContext that)) {
            return false;
        }
        return traceId.equals(that.traceId);
    }

    @Override
    public int hashCode() {
        return traceId.hashCode();
    }

    private static String validate(String value) {
        String nonNullValue = Objects.requireNonNull(value, "traceId");
        if (nonNullValue.isBlank()) {
            throw new IllegalArgumentException("traceId must not be blank");
        }
        if (nonNullValue.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("traceId is too long");
        }
        for (int index = 0; index < nonNullValue.length(); index++) {
            if (!isSafeCharacter(nonNullValue.charAt(index))) {
                throw new IllegalArgumentException("traceId contains an unsafe character");
            }
        }
        return nonNullValue;
    }

    private static boolean isSafeCharacter(char value) {
        return value >= 'a' && value <= 'z'
                || value >= 'A' && value <= 'Z'
                || value >= '0' && value <= '9'
                || value == '-'
                || value == '_'
                || value == '.';
    }
}
