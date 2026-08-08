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

package com.lamprism.luxspec.security.authorization;

import com.lamprism.luxspec.ErrorCode;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Represents an ordinary resource authorization outcome.
 *
 * @author RollW
 */
public final class AuthorizationDecision {
    private static final AuthorizationDecision ALLOWED = new AuthorizationDecision(true, null);

    private final boolean allowed;
    private final ErrorCode reasonCode;

    private AuthorizationDecision(boolean allowed, @Nullable ErrorCode reasonCode) {
        this.allowed = allowed;
        this.reasonCode = reasonCode;
    }

    /**
     * Returns the shared allowed authorization decision.
     *
     * @return the allowed decision
     */
    public static AuthorizationDecision allowed() {
        return ALLOWED;
    }

    /**
     * Creates a denied authorization decision.
     *
     * @param reasonCode the stable denial reason
     * @return the denied decision
     */
    public static AuthorizationDecision denied(ErrorCode reasonCode) {
        return new AuthorizationDecision(false, Objects.requireNonNull(reasonCode, "reasonCode"));
    }

    /**
     * Reports whether authorization was allowed.
     *
     * @return whether the action is allowed
     */
    public boolean isAllowed() {
        return allowed;
    }

    /**
     * Returns the stable denial reason when authorization was denied.
     *
     * @return the denial reason, or null when authorization was allowed
     */
    public @Nullable ErrorCode getReasonCode() {
        return reasonCode;
    }
}
