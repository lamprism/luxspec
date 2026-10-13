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

import com.lamprism.luxspec.security.crypto.KeyEntry;
import com.lamprism.luxspec.security.crypto.KeySet;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtJsonWebKeySetTest {
    @Test
    void publishesOnlyPublicAsymmetricVerificationKeys() throws GeneralSecurityException {
        KeyPair rsa = keyPair("RSA");
        KeyPair ec = ecKeyPair();
        KeyPair ed = keyPair("Ed25519");
        KeySet keySet = KeySet.forVerification(List.of(
                new KeyEntry("rsa", rsa.getPublic()),
                new KeyEntry("ec", ec.getPublic()),
                new KeyEntry("ed", ed.getPublic()),
                new KeyEntry(
                        "hmac",
                        new SecretKeySpec(
                                "hmac-key-material".getBytes(StandardCharsets.US_ASCII),
                                "HmacSHA256"
                        )
                )
        ));

        Map<String, Object> document = new NimbusJwkSetProjection().project(keySet);
        List<?> keys = (List<?>) document.get("keys");

        assertNotNull(keys);
        assertEquals(3, keys.size());
        assertKey(keys, "rsa", "RSA", null);
        assertKey(keys, "ec", "EC", "P-256");
        assertKey(keys, "ed", "OKP", "Ed25519");
        assertFalse(keys.stream().anyMatch(key -> "hmac".equals(((Map<?, ?>) key).get("kid"))));
    }

    private static KeyPair keyPair(String algorithm) throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(algorithm);
        if ("RSA".equals(algorithm)) {
            generator.initialize(2048);
        }
        return generator.generateKeyPair();
    }

    private static KeyPair ecKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }

    private static void assertKey(List<?> keys, String keyId, String keyType, String curve) {
        Map<?, ?> key = keys.stream()
                .map(Map.class::cast)
                .filter(candidate -> keyId.equals(candidate.get("kid")))
                .findFirst()
                .orElseThrow();
        assertEquals(keyType, key.get("kty"));
        assertEquals("sig", key.get("use"));
        if (curve == null) {
            assertNotNull(key.get("n"));
            assertNotNull(key.get("e"));
        } else {
            assertEquals(curve, key.get("crv"));
            assertNotNull(key.get("x"));
        }
    }
}
