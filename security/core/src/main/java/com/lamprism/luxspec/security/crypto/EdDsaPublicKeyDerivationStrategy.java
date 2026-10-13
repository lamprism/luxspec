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

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed448PrivateKeyParameters;

final class EdDsaPublicKeyDerivationStrategy extends AbstractPublicKeyDerivationStrategy {
    @Override
    protected DerivedPublicKey deriveParameters(
            AsymmetricKeyParameter privateKey,
            String algorithm
    ) {
        if (privateKey instanceof Ed25519PrivateKeyParameters ed25519PrivateKey) {
            if ("ED448".equals(algorithm)) {
                throw incompatibleKey(algorithm);
            }
            return new DerivedPublicKey(ed25519PrivateKey.generatePublicKey(), "Ed25519");
        }
        if (privateKey instanceof Ed448PrivateKeyParameters ed448PrivateKey) {
            if ("ED25519".equals(algorithm)) {
                throw incompatibleKey(algorithm);
            }
            return new DerivedPublicKey(ed448PrivateKey.generatePublicKey(), "Ed448");
        }
        throw incompatibleKey(algorithm);
    }
}
