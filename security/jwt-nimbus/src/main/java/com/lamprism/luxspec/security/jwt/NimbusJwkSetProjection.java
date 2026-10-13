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
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;

import javax.crypto.SecretKey;
import java.security.Key;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.EdECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Projects a provider-neutral Luxspec key-set snapshot into a Nimbus-backed public JWKS
 * document.
 *
 * <p>Only asymmetric verification keys are published. Symmetric verification material is never
 * exposed through this projection.</p>
 *
 * @author RollW
 */
public final class NimbusJwkSetProjection {
    /**
     * Creates a Nimbus JWK-set projection.
     */
    public NimbusJwkSetProjection() {
    }

    /**
     * Projects a key-set snapshot into a JSON-compatible JWKS document.
     *
     * @param keySet the current immutable key-set snapshot
     * @return the public JWKS document
     */
    public Map<String, Object> project(KeySet keySet) {
        KeySet nonNullKeySet = Objects.requireNonNull(keySet, "keySet");
        List<JWK> keys = new ArrayList<>();
        for (KeyEntry entry : nonNullKeySet.getEntries()) {
            JWK key = publicKey(entry);
            if (key != null) {
                keys.add(key);
            }
        }
        return new JWKSet(List.copyOf(keys)).toJSONObject();
    }

    private static JWK publicKey(KeyEntry entry) {
        Key verificationKey = entry.getVerificationKey();
        if (verificationKey instanceof SecretKey) {
            return null;
        }
        if (verificationKey instanceof RSAPublicKey rsaKey) {
            return new RSAKey.Builder(rsaKey)
                    .keyID(entry.getId())
                    .keyUse(KeyUse.SIGNATURE)
                    .build();
        }
        if (verificationKey instanceof ECPublicKey ecKey) {
            return new ECKey.Builder(Curve.forECParameterSpec(ecKey.getParams()), ecKey)
                    .keyID(entry.getId())
                    .keyUse(KeyUse.SIGNATURE)
                    .build();
        }
        if (verificationKey instanceof EdECPublicKey edKey) {
            return edKey(edKey, entry.getId());
        }
        throw new IllegalArgumentException(
                "JWKS publication does not support verification key type: "
                        + verificationKey.getClass().getName()
        );
    }

    private static OctetKeyPair edKey(EdECPublicKey key, String keyId) {
        String algorithm = key.getParams().getName();
        Curve curve;
        int rawLength;
        if ("Ed25519".equals(algorithm)) {
            curve = Curve.Ed25519;
            rawLength = 32;
        } else if ("Ed448".equals(algorithm)) {
            curve = Curve.Ed448;
            rawLength = 57;
        } else {
            throw new IllegalArgumentException("Unsupported Edwards curve: " + algorithm);
        }
        byte[] encoded = Objects.requireNonNull(key.getEncoded(), "encoded public key");
        if (encoded.length < rawLength) {
            throw new IllegalArgumentException("Encoded Edwards public key is too short");
        }
        byte[] raw = Arrays.copyOfRange(encoded, encoded.length - rawLength, encoded.length);
        return new OctetKeyPair.Builder(curve, Base64URL.encode(raw))
                .keyID(keyId)
                .keyUse(KeyUse.SIGNATURE)
                .build();
    }
}
