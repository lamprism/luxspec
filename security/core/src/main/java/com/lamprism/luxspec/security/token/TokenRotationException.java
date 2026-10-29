/*
 * Copyright (C) Lamprism
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lamprism.luxspec.security.token;

import java.util.Objects;

/**
 * Reports an authoritative token rotation rejection without exposing token values.
 *
 * @author RollW
 */
public final class TokenRotationException extends RuntimeException {
    private final TokenRotationRejection reason;

    /**
     * Creates a rejection with its internal reason.
     *
     * @param reason the rejection reason
     */
    public TokenRotationException(TokenRotationRejection reason) {
        super("Token rotation was rejected");
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    /**
     * Returns the authoritative reason for the rejection.
     *
     * @return the rejection reason
     */
    public TokenRotationRejection getReason() {
        return reason;
    }
}
