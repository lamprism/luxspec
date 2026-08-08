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

package com.lamprism.luxspec.security.token;

import java.time.Instant;
import java.util.Objects;

/**
 * Describes one typed old-to-new token digest transition.
 *
 * @param <T> the rotated token value type
 * @author RollW
 */
public final class TokenRotation<T extends Token> {
    private final TokenDigest<T> presentedDigest;
    private final TokenDigest<T> successorDigest;
    private final Instant rotatedAt;

    /**
     * Creates a transition between two distinct Digests of the same Token kind.
     *
     * @param presentedDigest the presented Token Digest
     * @param successorDigest the successor Token Digest
     * @param rotatedAt       the rotation time
     */
    public TokenRotation(
            TokenDigest<T> presentedDigest,
            TokenDigest<T> successorDigest,
            Instant rotatedAt
    ) {
        this.presentedDigest = Objects.requireNonNull(presentedDigest, "presentedDigest");
        this.successorDigest = Objects.requireNonNull(successorDigest, "successorDigest");
        this.rotatedAt = Objects.requireNonNull(rotatedAt, "rotatedAt");
        if (!this.presentedDigest.getTokenKind().equals(this.successorDigest.getTokenKind())) {
            throw new IllegalArgumentException("Token rotation digests must have the same kind");
        }
        if (this.presentedDigest.equals(this.successorDigest)) {
            throw new IllegalArgumentException("Token rotation successor must differ from the presented digest");
        }
    }

    /**
     * Returns the presented Token Digest.
     *
     * @return the presented Digest
     */
    public TokenDigest<T> getPresentedDigest() {
        return presentedDigest;
    }

    /**
     * Returns the successor Token Digest.
     *
     * @return the successor Digest
     */
    public TokenDigest<T> getSuccessorDigest() {
        return successorDigest;
    }

    /**
     * Returns the rotation time.
     *
     * @return the rotation time
     */
    public Instant getRotatedAt() {
        return rotatedAt;
    }
}
