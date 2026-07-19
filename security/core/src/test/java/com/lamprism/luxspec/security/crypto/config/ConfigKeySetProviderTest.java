package com.lamprism.luxspec.security.crypto.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lamprism.luxspec.config.ConfigEntry;
import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigLayer;
import com.lamprism.luxspec.config.ConfigSource;
import com.lamprism.luxspec.config.ConfigSourceCapability;
import com.lamprism.luxspec.config.ConfigSourceId;
import com.lamprism.luxspec.config.LayeredConfigReader;
import com.lamprism.luxspec.security.crypto.KeyEntry;
import com.lamprism.luxspec.security.crypto.KeySet;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConfigKeySetProviderTest {
    @Test
    void loadsAnActiveSecretKey() {
        byte[] secret = "01234567890123456789012345678901".getBytes(StandardCharsets.US_ASCII);
        Map<ConfigKey, ConfigEntry> entries = new HashMap<>();
        Map<String, String> keySetParameters = Map.of("key-set", "access");
        Map<String, String> keyParameters = Map.of("key-set", "access", "key-id", "primary");
        entries.put(ConfigKeySetSpecs.ACTIVE_KEY_ID.bind(keySetParameters).getKey(), ConfigEntry.present("primary"));
        entries.put(ConfigKeySetSpecs.KEY_IDS.bind(keySetParameters).getKey(), ConfigEntry.present(List.of("primary")));
        entries.put(ConfigKeySetSpecs.TYPE.bind(keyParameters).getKey(), ConfigEntry.present("secret"));
        entries.put(ConfigKeySetSpecs.ALGORITHM.bind(keyParameters).getKey(), ConfigEntry.present("HmacSHA256"));
        entries.put(
                ConfigKeySetSpecs.SECRET.bind(keyParameters).getKey(),
                ConfigEntry.present(Base64.getEncoder().encodeToString(secret))
        );

        KeySet keySet = provider(entries).get("access");
        KeyEntry keyEntry = keySet.getActiveKey().orElseThrow();

        assertEquals("primary", keyEntry.getId());
        assertArrayEquals(secret, keyEntry.getSigningKey().orElseThrow().getEncoded());
        assertArrayEquals(secret, keyEntry.getVerificationKey().getEncoded());
    }

    @Test
    void loadsKeyPairAndVerificationOnlyPublicKey() throws GeneralSecurityException {
        KeyPair keyPair = rsaKeyPair();
        Map<ConfigKey, ConfigEntry> entries = new HashMap<>();
        Map<String, String> keySetParameters = Map.of("key-set", "access");
        Map<String, String> signingParameters = Map.of("key-set", "access", "key-id", "signing");
        Map<String, String> remoteParameters = Map.of("key-set", "access", "key-id", "remote");
        entries.put(ConfigKeySetSpecs.ACTIVE_KEY_ID.bind(keySetParameters).getKey(), ConfigEntry.present("signing"));
        entries.put(
                ConfigKeySetSpecs.KEY_IDS.bind(keySetParameters).getKey(),
                ConfigEntry.present(List.of("signing", "remote"))
        );
        entries.put(ConfigKeySetSpecs.TYPE.bind(signingParameters).getKey(), ConfigEntry.present("key-pair"));
        entries.put(ConfigKeySetSpecs.ALGORITHM.bind(signingParameters).getKey(), ConfigEntry.present("RSA"));
        entries.put(
                ConfigKeySetSpecs.PRIVATE_KEY.bind(signingParameters).getKey(),
                ConfigEntry.present(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()))
        );
        entries.put(
                ConfigKeySetSpecs.PUBLIC_KEY.bind(signingParameters).getKey(),
                ConfigEntry.present(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()))
        );
        entries.put(ConfigKeySetSpecs.TYPE.bind(remoteParameters).getKey(), ConfigEntry.present("public-key"));
        entries.put(ConfigKeySetSpecs.ALGORITHM.bind(remoteParameters).getKey(), ConfigEntry.present("RSA"));
        entries.put(
                ConfigKeySetSpecs.PUBLIC_KEY.bind(remoteParameters).getKey(),
                ConfigEntry.present(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()))
        );

        KeySet keySet = provider(entries).get("access");
        KeyEntry signingEntry = keySet.getActiveKey().orElseThrow();
        KeyEntry remoteEntry = keySet.findById("remote").orElseThrow();

        assertArrayEquals(keyPair.getPrivate().getEncoded(), signingEntry.getSigningKey().orElseThrow().getEncoded());
        assertArrayEquals(keyPair.getPublic().getEncoded(), signingEntry.getVerificationKey().getEncoded());
        assertFalse(remoteEntry.getSigningKey().isPresent());
        assertArrayEquals(keyPair.getPublic().getEncoded(), remoteEntry.getVerificationKey().getEncoded());
    }

    @Test
    void rejectsAnUnavailableKeySetName() {
        assertThrows(IllegalArgumentException.class, () -> provider(Map.of()).get("access"));
    }

    private static ConfigKeySetProvider provider(Map<ConfigKey, ConfigEntry> entries) {
        LayeredConfigReader reader = new LayeredConfigReader(List.of(new ConfigLayer(new MemorySource(entries))));
        return new ConfigKeySetProvider(reader);
    }

    private static KeyPair rsaKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static final class MemorySource implements ConfigSource {
        private final Map<ConfigKey, ConfigEntry> entries;

        private MemorySource(Map<ConfigKey, ConfigEntry> entries) {
            this.entries = Map.copyOf(entries);
        }

        @Override
        public ConfigSourceId getId() {
            return ConfigSourceId.of("secure-memory");
        }

        @Override
        public Set<ConfigSourceCapability> getCapabilities() {
            return Set.of(ConfigSourceCapability.READ, ConfigSourceCapability.SECURE);
        }

        @Override
        public ConfigEntry get(ConfigKey key) {
            return entries.getOrDefault(key, ConfigEntry.absent());
        }
    }
}
