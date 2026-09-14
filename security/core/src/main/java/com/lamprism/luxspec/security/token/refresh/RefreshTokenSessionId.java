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

package com.lamprism.luxspec.security.token.refresh;

import java.util.Objects;

/**
 * Identifies one stable Refresh Token Session.
 *
 * @author RollW
 */
public final class RefreshTokenSessionId {
    private final String value;

    /**
     * Creates a Refresh Token Session identifier.
     *
     * @param value the non-blank identifier value
     */
    public RefreshTokenSessionId(String value) {
        this.value = Objects.requireNonNull(value, "value");
        if (this.value.isBlank()) {
            throw new IllegalArgumentException("Refresh Token Session ID must not be blank");
        }
    }

    /**
     * Returns the stable identifier value.
     *
     * @return the identifier value
     */
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof RefreshTokenSessionId sessionId)) {
            return false;
        }
        return value.equals(sessionId.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
