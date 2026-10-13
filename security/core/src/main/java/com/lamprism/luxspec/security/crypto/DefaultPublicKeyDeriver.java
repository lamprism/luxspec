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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Derives JCA public keys for RSA, DSA, EC, Ed25519, and Ed448 private keys.
 *
 * <p>The implementation uses Bouncy Castle's lightweight parameter API without registering a
 * global JCA provider. The final key is created by the JCA {@code KeyFactory}, so callers only
 * depend on standard {@link PrivateKey} and {@link PublicKey} contracts.</p>
 *
 * @author RollW
 */
public class DefaultPublicKeyDeriver implements PublicKeyDeriver {
    private static final Map<String, PublicKeyDerivationStrategy> STRATEGIES = Map.of(
            "RSA", new RsaPublicKeyDerivationStrategy(),
            "DSA", new DsaPublicKeyDerivationStrategy(),
            "EC", new EcPublicKeyDerivationStrategy(),
            "ED25519", new EdDsaPublicKeyDerivationStrategy(),
            "ED448", new EdDsaPublicKeyDerivationStrategy(),
            "EDDSA", new EdDsaPublicKeyDerivationStrategy()
    );

    /**
     * Creates the default public-key deriver.
     */
    public DefaultPublicKeyDeriver() {
    }

    @Override
    public PublicKey derivePublicKey(PrivateKey privateKey) {
        PrivateKey nonNullPrivateKey = Objects.requireNonNull(privateKey, "privateKey");
        String algorithm = nonNullPrivateKey.getAlgorithm();
        if (algorithm == null || algorithm.isBlank()) {
            throw new PublicKeyDerivationException("Private key algorithm is unavailable");
        }
        String normalizedAlgorithm = algorithm.toUpperCase(Locale.ROOT);
        PublicKeyDerivationStrategy strategy = STRATEGIES.get(normalizedAlgorithm);
        if (strategy == null) {
            throw new PublicKeyDerivationException("Private key algorithm is unsupported: " + algorithm);
        }
        return strategy.derivePublicKey(nonNullPrivateKey, normalizedAlgorithm);
    }
}
