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

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Derives a public key from its corresponding private key material.
 *
 * <p>The operation is only possible when the private key encoding contains enough parameters to
 * reconstruct the public key. Implementations should reject unsupported or incomplete private key
 * material instead of guessing a public key.</p>
 *
 * @author RollW
 */
@FunctionalInterface
public interface PublicKeyDeriver {
    /**
     * Derives the public key corresponding to the supplied private key.
     *
     * @param privateKey the private key
     * @return the derived public key
     * @throws PublicKeyDerivationException when the key cannot be derived
     */
    PublicKey derivePublicKey(PrivateKey privateKey);
}
