package com.lamprism.luxspec.security.crypto.config;

import com.lamprism.luxspec.config.ConfigReadOption;
import com.lamprism.luxspec.config.ConfigReader;
import com.lamprism.luxspec.config.ConfigSpec;
import com.lamprism.luxspec.security.crypto.KeyEntry;
import com.lamprism.luxspec.security.crypto.KeySet;
import com.lamprism.luxspec.security.crypto.KeySetProvider;
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
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Resolves named JCA key sets from typed Luxspec configuration.
 *
 * @author RollW
 */
public final class ConfigKeySetProvider implements KeySetProvider {
    private final ConfigReader reader;

    /**
     * Creates a provider backed by typed Luxspec configuration.
     *
     * @param reader the typed configuration reader
     */
    public ConfigKeySetProvider(ConfigReader reader) {
        this.reader = Objects.requireNonNull(reader, "reader");
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
        Optional<String> activeKeyId = readOptional(ConfigKeySetSpecs.ACTIVE_KEY_ID.bind(keySetParameters));
        Optional<List<String>> keyIds = readOptional(ConfigKeySetSpecs.KEY_IDS.bind(keySetParameters));
        if (activeKeyId.isEmpty() && keyIds.isEmpty()) {
            throw new IllegalArgumentException("Configured key set is not available: " + nonBlankKeySetName);
        }
        List<KeyEntry> entries = new ArrayList<>();
        for (String keyId : keyIds.orElseThrow(
                () -> new IllegalStateException("Configured key set does not declare key IDs")
        )) {
            entries.add(readEntry(nonBlankKeySetName, keyId));
        }
        if (activeKeyId.isPresent()) {
            return KeySet.withActiveKey(activeKeyId.orElseThrow(), entries);
        }
        return KeySet.forVerification(entries);
    }

    private KeyEntry readEntry(String keySetName, String keyId) {
        String nonBlankKeyId = requireText(keyId, "keyId");
        Map<String, String> parameters = Map.of(
                "key-set", keySetName,
                "key-id", nonBlankKeyId
        );
        String type = requireText(read(ConfigKeySetSpecs.TYPE.bind(parameters)), "type");
        String algorithm = requireText(read(ConfigKeySetSpecs.ALGORITHM.bind(parameters)), "algorithm");
        if ("secret".equals(type)) {
            return secretEntry(nonBlankKeyId, algorithm, read(ConfigKeySetSpecs.SECRET.bind(parameters)));
        }
        if ("key-pair".equals(type)) {
            return keyPairEntry(
                    nonBlankKeyId,
                    algorithm,
                    read(ConfigKeySetSpecs.PRIVATE_KEY.bind(parameters)),
                    read(ConfigKeySetSpecs.PUBLIC_KEY.bind(parameters))
            );
        }
        if ("public-key".equals(type)) {
            return publicKeyEntry(
                    nonBlankKeyId,
                    algorithm,
                    read(ConfigKeySetSpecs.PUBLIC_KEY.bind(parameters))
            );
        }
        throw new IllegalArgumentException("Key material type is unsupported: " + type);
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
            String encodedPublicKey
    ) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(
                    decode(encodedPrivateKey, "privateKey")
            ));
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(
                    decode(encodedPublicKey, "publicKey")
            ));
            return new KeyEntry(keyId, privateKey, publicKey);
        } catch (GeneralSecurityException exception) {
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

    private <T> T read(ConfigSpec<T> spec) {
        return readOptional(spec).orElseThrow(
                () -> new IllegalStateException("Required key-set configuration is not available: " + spec.getKey().getValue())
        );
    }

    private <T> Optional<T> readOptional(ConfigSpec<T> spec) {
        return reader.get(spec, ConfigReadOption.FRESH).getValue();
    }

    private String requireText(String value, String name) {
        String nonNullValue = Objects.requireNonNull(value, name);
        if (nonNullValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return nonNullValue;
    }
}
