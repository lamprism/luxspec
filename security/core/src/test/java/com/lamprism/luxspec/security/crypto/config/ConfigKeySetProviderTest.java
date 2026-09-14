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

import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.runtime.LayeredConfigReader;
import com.lamprism.luxspec.config.source.ConfigEntry;
import com.lamprism.luxspec.config.source.ConfigSource;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.ConfigSourceScope;
import com.lamprism.luxspec.security.crypto.KeyEntry;
import com.lamprism.luxspec.security.crypto.KeySet;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigKeySetProviderTest {
    @Test
    void loadsAnActiveSecretKey() {
        byte[] secret = "01234567890123456789012345678901".getBytes(StandardCharsets.US_ASCII);
        Map<ConfigKey, ConfigEntry> entries = new HashMap<>();
        Map<String, String> keySetParameters = Map.of("key-set", "access");
        Map<String, String> keyParameters = Map.of("key-set", "access", "key-id", "primary");
        entries.put(ConfigKeySetProvider.ACTIVE_KEY_ID.bind(keySetParameters).getKey(), ConfigEntry.present("primary"));
        entries.put(ConfigKeySetProvider.KEY_IDS.bind(keySetParameters).getKey(), ConfigEntry.present(List.of("primary")));
        entries.put(
                ConfigKeySetProvider.TYPE.bind(keyParameters).getKey(),
                typeEntry(ConfigKeyMaterialType.SECRET)
        );
        entries.put(ConfigKeySetProvider.ALGORITHM.bind(keyParameters).getKey(), ConfigEntry.present("HmacSHA256"));
        entries.put(
                ConfigKeySetProvider.SECRET.bind(keyParameters).getKey(),
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
        entries.put(ConfigKeySetProvider.ACTIVE_KEY_ID.bind(keySetParameters).getKey(), ConfigEntry.present("signing"));
        entries.put(
                ConfigKeySetProvider.KEY_IDS.bind(keySetParameters).getKey(),
                ConfigEntry.present(List.of("signing", "remote"))
        );
        entries.put(
                ConfigKeySetProvider.TYPE.bind(signingParameters).getKey(),
                typeEntry(ConfigKeyMaterialType.KEY_PAIR)
        );
        entries.put(ConfigKeySetProvider.ALGORITHM.bind(signingParameters).getKey(), ConfigEntry.present("RSA"));
        entries.put(
                ConfigKeySetProvider.PRIVATE_KEY.bind(signingParameters).getKey(),
                ConfigEntry.present(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()))
        );
        entries.put(
                ConfigKeySetProvider.PUBLIC_KEY.bind(signingParameters).getKey(),
                ConfigEntry.present(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()))
        );
        entries.put(
                ConfigKeySetProvider.TYPE.bind(remoteParameters).getKey(),
                typeEntry(ConfigKeyMaterialType.PUBLIC_KEY)
        );
        entries.put(ConfigKeySetProvider.ALGORITHM.bind(remoteParameters).getKey(), ConfigEntry.present("RSA"));
        entries.put(
                ConfigKeySetProvider.PUBLIC_KEY.bind(remoteParameters).getKey(),
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
    void derivesMissingPublicKeyForConfiguredKeyPair() throws GeneralSecurityException {
        KeyPair keyPair = rsaKeyPair();
        Map<ConfigKey, ConfigEntry> entries = new HashMap<>();
        Map<String, String> keySetParameters = Map.of("key-set", "access");
        Map<String, String> keyParameters = Map.of("key-set", "access", "key-id", "signing");
        entries.put(ConfigKeySetProvider.ACTIVE_KEY_ID.bind(keySetParameters).getKey(), ConfigEntry.present("signing"));
        entries.put(ConfigKeySetProvider.KEY_IDS.bind(keySetParameters).getKey(), ConfigEntry.present(List.of("signing")));
        entries.put(
                ConfigKeySetProvider.TYPE.bind(keyParameters).getKey(),
                typeEntry(ConfigKeyMaterialType.KEY_PAIR)
        );
        entries.put(ConfigKeySetProvider.ALGORITHM.bind(keyParameters).getKey(), ConfigEntry.present("RSA"));
        entries.put(
                ConfigKeySetProvider.PRIVATE_KEY.bind(keyParameters).getKey(),
                ConfigEntry.present(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()))
        );

        KeyEntry keyEntry = provider(entries).get("access").getActiveKey().orElseThrow();

        assertArrayEquals(keyPair.getPrivate().getEncoded(), keyEntry.getSigningKey().orElseThrow().getEncoded());
        assertArrayEquals(keyPair.getPublic().getEncoded(), keyEntry.getVerificationKey().getEncoded());
    }

    @Test
    void rejectsAnUnavailableKeySetName() {
        assertThrows(IllegalArgumentException.class, () -> provider(Map.of()).get("access"));
    }

    @Test
    void definesDescriptionsForEveryBuiltInSetting() {
        for (Locale locale : List.of(Locale.US, Locale.SIMPLIFIED_CHINESE)) {
            assertFalse(ConfigKeySetProvider.definitions().stream()
                    .map(spec -> spec.getDescription().resolve(locale))
                    .anyMatch(String::isBlank));
        }
    }

    private static ConfigKeySetProvider provider(Map<ConfigKey, ConfigEntry> entries) {
        LayeredConfigReader reader = new LayeredConfigReader(List.of(new MemorySource(entries)));
        return new ConfigKeySetProvider(reader);
    }

    private static ConfigEntry typeEntry(ConfigKeyMaterialType type) {
        return ConfigEntry.present(ConfigKeySetProvider.TYPE.getCodec().encode(type));
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
        public ConfigSourceScope getScope() {
            return ConfigSourceScope.RUNTIME;
        }

        @Override
        public ConfigEntry get(@NonNull ConfigKey key) {
            return entries.getOrDefault(key, ConfigEntry.absent());
        }
    }
}
