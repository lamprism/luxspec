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
import org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed448PrivateKeyParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.math.ec.ECPoint;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Objects;

/**
 * Derives JCA public keys for RSA, DSA, EC, Ed25519, and Ed448 private keys.
 *
 * <p>The implementation uses Bouncy Castle's lightweight parameter API without registering a
 * global JCA provider. The final key is created by the JCA {@link KeyFactory}, so callers only
 * depend on standard {@link PrivateKey} and {@link PublicKey} contracts.</p>
 *
 * @author RollW
 */
public class DefaultPublicKeyDeriver implements PublicKeyDeriver {
    /**
     * Creates the default public-key deriver.
     */
    public DefaultPublicKeyDeriver() {
    }

    @Override
    public PublicKey derivePublicKey(PrivateKey privateKey) {
        PrivateKey nonNullPrivateKey = Objects.requireNonNull(privateKey, "privateKey");
        byte[] encodedKey = nonNullPrivateKey.getEncoded();
        if (encodedKey == null || encodedKey.length == 0) {
            throw new PublicKeyDerivationException("Private key encoding is unavailable");
        }
        try {
            AsymmetricKeyParameter privateKeyParameters = PrivateKeyFactory.createKey(encodedKey);
            return derivePublicKey(privateKeyParameters, nonNullPrivateKey.getAlgorithm());
        } catch (IOException | GeneralSecurityException | IllegalArgumentException exception) {
            throw new PublicKeyDerivationException("Failed to derive public key", exception);
        }
    }

    private static PublicKey derivePublicKey(AsymmetricKeyParameter privateKey, String algorithm)
            throws GeneralSecurityException, IOException {
        if (privateKey instanceof RSAPrivateCrtKeyParameters rsaPrivateKey) {
            RSAKeyParameters rsaPublicKey = new RSAKeyParameters(
                    false,
                    rsaPrivateKey.getModulus(),
                    rsaPrivateKey.getPublicExponent()
            );
            return toJcaPublicKey(rsaPublicKey, "RSA");
        }
        if (privateKey instanceof RSAKeyParameters) {
            throw new PublicKeyDerivationException("RSA private key does not contain a public exponent");
        }
        if (privateKey instanceof DSAPrivateKeyParameters dsaPrivateKey) {
            BigInteger publicKeyValue = dsaPrivateKey.getParameters().getG()
                    .modPow(dsaPrivateKey.getX(), dsaPrivateKey.getParameters().getP());
            DSAPublicKeyParameters dsaPublicKey = new DSAPublicKeyParameters(
                    publicKeyValue,
                    dsaPrivateKey.getParameters()
            );
            return toJcaPublicKey(dsaPublicKey, "DSA");
        }
        if (privateKey instanceof ECPrivateKeyParameters ecPrivateKey) {
            ECPoint publicKeyPoint = ecPrivateKey.getParameters().getG()
                    .multiply(ecPrivateKey.getD())
                    .normalize();
            ECPublicKeyParameters ecPublicKey = new ECPublicKeyParameters(
                    publicKeyPoint,
                    ecPrivateKey.getParameters()
            );
            return toJcaPublicKey(ecPublicKey, "EC");
        }
        if (privateKey instanceof Ed25519PrivateKeyParameters ed25519PrivateKey) {
            return toJcaPublicKey(ed25519PrivateKey.generatePublicKey(), "Ed25519");
        }
        if (privateKey instanceof Ed448PrivateKeyParameters ed448PrivateKey) {
            return toJcaPublicKey(ed448PrivateKey.generatePublicKey(), "Ed448");
        }
        throw new PublicKeyDerivationException("Private key algorithm is unsupported: " + algorithm);
    }

    private static PublicKey toJcaPublicKey(
            AsymmetricKeyParameter publicKey,
            String algorithm
    ) throws GeneralSecurityException, IOException {
        byte[] encodedKey = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(publicKey).getEncoded();
        return KeyFactory.getInstance(algorithm).generatePublic(new X509EncodedKeySpec(encodedKey));
    }
}
