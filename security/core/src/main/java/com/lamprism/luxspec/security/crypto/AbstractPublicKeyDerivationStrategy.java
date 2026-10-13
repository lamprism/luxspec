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
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

abstract class AbstractPublicKeyDerivationStrategy implements PublicKeyDerivationStrategy {
    private static final Provider BOUNCY_CASTLE_PROVIDER = new BouncyCastleProvider();

    @Override
    public final PublicKey derivePublicKey(PrivateKey privateKey, String algorithm) {
        byte[] encodedKey = privateKey.getEncoded();
        if (encodedKey == null || encodedKey.length == 0) {
            throw new PublicKeyDerivationException("Private key encoding is unavailable");
        }
        try {
            AsymmetricKeyParameter privateKeyParameters = PrivateKeyFactory.createKey(encodedKey);
            DerivedPublicKey derivedKey = deriveParameters(privateKeyParameters, algorithm);
            byte[] encodedPublicKey = SubjectPublicKeyInfoFactory
                    .createSubjectPublicKeyInfo(derivedKey.parameters())
                    .getEncoded();
            return KeyFactory.getInstance(derivedKey.algorithm(), BOUNCY_CASTLE_PROVIDER)
                    .generatePublic(new X509EncodedKeySpec(encodedPublicKey));
        } catch (PublicKeyDerivationException exception) {
            throw exception;
        } catch (IOException | GeneralSecurityException | IllegalArgumentException exception) {
            throw new PublicKeyDerivationException(
                    "Failed to derive public key for algorithm: " + algorithm,
                    exception
            );
        }
    }

    protected abstract DerivedPublicKey deriveParameters(
            AsymmetricKeyParameter privateKey,
            String algorithm
    );

    protected static PublicKeyDerivationException incompatibleKey(String algorithm) {
        return new PublicKeyDerivationException(
                "Encoded private key does not match declared algorithm: " + algorithm
        );
    }

    protected record DerivedPublicKey(AsymmetricKeyParameter parameters, String algorithm) {
    }
}
