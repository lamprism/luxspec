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

package com.lamprism.luxspec.security.jwt;

import com.nimbusds.jose.JWSAlgorithm;

import java.security.Key;
import java.util.Objects;
import java.util.Set;

/**
 * Selects a signing algorithm and restricts algorithms trusted for verification.
 *
 * <p>The adapter independently checks its supported algorithms and key compatibility. A token's
 * header cannot expand the algorithms trusted by this policy.</p>
 *
 * @author RollW
 */
public interface JwtAlgorithmPolicy {
    /**
     * Uses the adapter's key-derived signing algorithm and supported verification algorithms.
     *
     * @return the standard policy
     */
    static JwtAlgorithmPolicy standard() {
        return new JwtAlgorithmPolicy() {
            @Override
            public JWSAlgorithm signingAlgorithm(Key key, JWSAlgorithm suggested) {
                return suggested;
            }

            @Override
            public boolean permitsVerification(Key key, JWSAlgorithm algorithm) {
                return true;
            }
        };
    }

    /**
     * Selects one signing algorithm and a fixed trusted verification set.
     *
     * @param signingAlgorithm  the signing algorithm
     * @param trustedAlgorithms the trusted verification algorithms
     * @return the explicit algorithm policy
     */
    static JwtAlgorithmPolicy fixed(JWSAlgorithm signingAlgorithm, Set<JWSAlgorithm> trustedAlgorithms) {
        JWSAlgorithm signing = Objects.requireNonNull(signingAlgorithm, "signingAlgorithm");
        Set<JWSAlgorithm> trusted = Set.copyOf(trustedAlgorithms);
        if (!trusted.contains(signing)) {
            throw new IllegalArgumentException("Signing algorithm must be trusted for verification");
        }
        return new JwtAlgorithmPolicy() {
            @Override
            public JWSAlgorithm signingAlgorithm(Key key, JWSAlgorithm suggested) {
                return signing;
            }

            @Override
            public boolean permitsVerification(Key key, JWSAlgorithm algorithm) {
                return trusted.contains(algorithm);
            }
        };
    }

    /**
     * Selects the algorithm for a trusted signing key.
     *
     * @param key       the trusted signing key
     * @param suggested the adapter's default for that key
     * @return the selected signing algorithm
     */
    JWSAlgorithm signingAlgorithm(Key key, JWSAlgorithm suggested);

    /**
     * Reports whether an algorithm may be verified with the trusted key.
     *
     * @param key       the trusted verification key
     * @param algorithm the algorithm to check
     * @return whether the algorithm is trusted
     */
    boolean permitsVerification(Key key, JWSAlgorithm algorithm);
}
