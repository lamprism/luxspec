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

package com.lamprism.luxspec.security.crypto;

import java.security.Key;
import java.util.Objects;
import java.util.Optional;

/**
 * Associates a stable key ID with verification material and optional signing material.
 *
 * @author RollW
 */
public final class KeyEntry {
    private final String id;
    private final Key verificationKey;
    private final Optional<Key> signingKey;

    /**
     * Creates an entry that can sign and verify cryptographic messages.
     *
     * @param id              the non-blank stable key ID
     * @param signingKey      the private or secret signing key
     * @param verificationKey the public or secret verification key
     */
    public KeyEntry(String id, Key signingKey, Key verificationKey) {
        this(id, verificationKey, Optional.of(Objects.requireNonNull(signingKey, "signingKey")));
    }

    /**
     * Creates an entry that can verify but cannot sign cryptographic messages.
     *
     * @param id              the non-blank stable key ID
     * @param verificationKey the public or secret verification key
     */
    public KeyEntry(String id, Key verificationKey) {
        this(id, verificationKey, Optional.empty());
    }

    private KeyEntry(String id, Key verificationKey, Optional<Key> signingKey) {
        this.id = requireId(id);
        this.verificationKey = Objects.requireNonNull(verificationKey, "verificationKey");
        this.signingKey = Objects.requireNonNull(signingKey, "signingKey");
    }

    /**
     * Returns the non-empty stable key ID.
     *
     * @return the key ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the public or secret verification key.
     *
     * @return the verification key
     */
    public Key getVerificationKey() {
        return verificationKey;
    }

    /**
     * Returns the private or secret signing key when this entry can sign.
     *
     * @return the optional signing key
     */
    public Optional<Key> getSigningKey() {
        return signingKey;
    }

    private static String requireId(String value) {
        String id = Objects.requireNonNull(value, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Key ID must not be blank");
        }
        return id;
    }
}
