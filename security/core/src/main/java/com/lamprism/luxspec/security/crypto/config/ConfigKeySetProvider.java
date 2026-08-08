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

package com.lamprism.luxspec.security.crypto.config;

import com.lamprism.luxspec.config.ConfigBinding;
import com.lamprism.luxspec.config.ConfigReader;
import com.lamprism.luxspec.security.crypto.DefaultPublicKeyDeriver;
import com.lamprism.luxspec.security.crypto.KeyEntry;
import com.lamprism.luxspec.security.crypto.KeySet;
import com.lamprism.luxspec.security.crypto.KeySetProvider;
import com.lamprism.luxspec.security.crypto.PublicKeyDeriver;
import org.jspecify.annotations.Nullable;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves named JCA key sets from typed Luxspec configuration.
 *
 * @author RollW
 */
public final class ConfigKeySetProvider implements KeySetProvider {
    private final ConfigReader reader;
    private final PublicKeyDeriver publicKeyDeriver;

    /**
     * Creates a provider backed by typed Luxspec configuration.
     *
     * @param reader the typed configuration reader
     */
    public ConfigKeySetProvider(ConfigReader reader) {
        this(reader, new DefaultPublicKeyDeriver());
    }

    /**
     * Creates a provider backed by typed Luxspec configuration and a custom public-key deriver.
     *
     * @param reader           the typed configuration reader
     * @param publicKeyDeriver the public-key deriver used when a key pair omits its public key
     */
    public ConfigKeySetProvider(ConfigReader reader, PublicKeyDeriver publicKeyDeriver) {
        this.reader = Objects.requireNonNull(reader, "reader");
        this.publicKeyDeriver = Objects.requireNonNull(publicKeyDeriver, "publicKeyDeriver");
    }

    /**
     * Returns one configured key set by name.
     *
     * @param keySetName the provider-local key-set name
     * @return the configured non-null key set
     * @throws IllegalArgumentException when the name has no definition
     */
    @Override
    public KeySet get(String keySetName) {
        String nonBlankKeySetName = requireText(keySetName, "keySetName");
        Map<String, String> keySetParameters = Map.of("key-set", nonBlankKeySetName);
        String activeKeyId = readOptional(ConfigKeySetSpecs.ACTIVE_KEY_ID.bind(keySetParameters));
        List<String> keyIds = readOptional(ConfigKeySetSpecs.KEY_IDS.bind(keySetParameters));
        if (activeKeyId == null && keyIds == null) {
            throw new IllegalArgumentException("Configured key set is not available: " + nonBlankKeySetName);
        }
        List<KeyEntry> entries = new ArrayList<>();
        if (keyIds == null) {
            throw new IllegalStateException("Configured key set does not declare key IDs");
        }
        for (String keyId : keyIds) {
            entries.add(readEntry(nonBlankKeySetName, keyId));
        }
        if (activeKeyId != null) {
            return KeySet.withActiveKey(activeKeyId, entries);
        }
        return KeySet.forVerification(entries);
    }

    private KeyEntry readEntry(String keySetName, String keyId) {
        String nonBlankKeyId = requireText(keyId, "keyId");
        Map<String, String> parameters = Map.of(
                "key-set", keySetName,
                "key-id", nonBlankKeyId
        );
        ConfigKeyMaterialType type = read(ConfigKeySetSpecs.TYPE.bind(parameters));
        String algorithm = requireText(read(ConfigKeySetSpecs.ALGORITHM.bind(parameters)), "algorithm");
        return switch (type) {
            case SECRET -> secretEntry(
                    nonBlankKeyId,
                    algorithm,
                    read(ConfigKeySetSpecs.SECRET.bind(parameters))
            );
            case KEY_PAIR -> keyPairEntry(
                    nonBlankKeyId,
                    algorithm,
                    read(ConfigKeySetSpecs.PRIVATE_KEY.bind(parameters)),
                    readOptional(ConfigKeySetSpecs.PUBLIC_KEY.bind(parameters))
            );
            case PUBLIC_KEY -> publicKeyEntry(
                    nonBlankKeyId,
                    algorithm,
                    read(ConfigKeySetSpecs.PUBLIC_KEY.bind(parameters))
            );
        };
    }

    private KeyEntry secretEntry(String keyId, String algorithm, String encodedSecret) {
        byte[] secret = decode(encodedSecret, "secret");
        SecretKey key = new SecretKeySpec(secret, algorithm);
        return new KeyEntry(keyId, key, key);
    }

    private KeyEntry keyPairEntry(
            String keyId,
            String algorithm,
            String encodedPrivateKey,
            @Nullable String encodedPublicKey
    ) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(
                    decode(encodedPrivateKey, "privateKey")
            ));
            PublicKey publicKey = encodedPublicKey == null
                    ? publicKeyDeriver.derivePublicKey(privateKey)
                    : keyFactory.generatePublic(new X509EncodedKeySpec(
                    decode(encodedPublicKey, "publicKey")
            ));
            return new KeyEntry(keyId, privateKey, publicKey);
        } catch (GeneralSecurityException | RuntimeException exception) {
            throw new IllegalArgumentException("Configured key pair is invalid", exception);
        }
    }

    private KeyEntry publicKeyEntry(String keyId, String algorithm, String encodedPublicKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(
                    decode(encodedPublicKey, "publicKey")
            ));
            return new KeyEntry(keyId, publicKey);
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Configured public key is invalid", exception);
        }
    }

    private byte[] decode(String encodedValue, String name) {
        byte[] decoded = Base64.getDecoder().decode(requireText(encodedValue, name));
        if (decoded.length == 0) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return decoded;
    }

    private <T> T read(ConfigBinding<T> binding) {
        T value = readOptional(binding);
        if (value == null) {
            throw new IllegalStateException(
                    "Required key-set configuration is not available: " + binding.getKey().getValue()
            );
        }
        return value;
    }

    private <T> @Nullable T readOptional(ConfigBinding<T> binding) {
        return reader.get(binding).getValue();
    }

    private String requireText(String value, String name) {
        String nonNullValue = Objects.requireNonNull(value, name);
        if (nonNullValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return nonNullValue;
    }
}
