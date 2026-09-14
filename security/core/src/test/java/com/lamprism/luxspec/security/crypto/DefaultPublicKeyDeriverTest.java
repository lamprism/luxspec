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

import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultPublicKeyDeriverTest {
    private final DefaultPublicKeyDeriver deriver = new DefaultPublicKeyDeriver();

    @Test
    void derivesSupportedPublicKeyAlgorithms() throws GeneralSecurityException {
        assertDerivedKeyMatches("RSA", 2048);
        assertDerivedKeyMatches("DSA", 2048);
        assertDerivedKeyMatches("EC", 256);
        assertDerivedKeyMatches("Ed25519", null);
        assertDerivedKeyMatches("Ed448", null);
    }

    @Test
    void rejectsUnsupportedPrivateKeyEncoding() {
        PrivateKey privateKey = new PrivateKey() {
            @Override
            public String getAlgorithm() {
                return "unsupported";
            }

            @Override
            public String getFormat() {
                return "PKCS#8";
            }

            @Override
            public byte[] getEncoded() {
                return new byte[]{1};
            }
        };

        assertThrows(PublicKeyDerivationException.class, () -> deriver.derivePublicKey(privateKey));
    }

    private void assertDerivedKeyMatches(String algorithm, Integer keySize) throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(algorithm);
        if (keySize != null) {
            generator.initialize(keySize);
        }
        KeyPair keyPair = generator.generateKeyPair();

        assertEquals(keyPair.getPublic(), deriver.derivePublicKey(keyPair.getPrivate()));
    }
}
