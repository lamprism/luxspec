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

import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.runtime.LayeredConfigReader;
import com.lamprism.luxspec.config.source.ConfigEntry;
import com.lamprism.luxspec.config.source.ConfigSource;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.ConfigSourceScope;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ConfigJwtAccessTokenSettingsSourceTest {
    @Test
    void loadsTypedAccessTokenSettingsAndKeySetReference() {
        Map<ConfigKey, ConfigEntry> entries = new HashMap<>();
        entries.put(ConfigJwtAccessTokenSettingsSource.ACCESS_TTL.bind().getKey(), ConfigEntry.present("PT10M"));
        entries.put(ConfigJwtAccessTokenSettingsSource.ACCESS_ISSUER.bind().getKey(), ConfigEntry.present("luxspec-test"));
        entries.put(ConfigJwtAccessTokenSettingsSource.ACCESS_AUDIENCES.bind().getKey(), ConfigEntry.present(List.of("api")));
        entries.put(ConfigJwtAccessTokenSettingsSource.KEY_SET_NAME.bind().getKey(), ConfigEntry.present("access"));
        LayeredConfigReader reader = new LayeredConfigReader(List.of(new MemorySource(entries)));
        ConfigJwtAccessTokenSettingsSource source = new ConfigJwtAccessTokenSettingsSource(reader);

        JwtAccessTokenConfiguration configuration = source.getConfiguration();

        assertEquals(Duration.ofMinutes(10), configuration.getLifetime());
        assertEquals("luxspec-test", configuration.getIssuer());
        assertEquals(Set.of("api"), configuration.getAudiences());
        assertEquals("access", source.getKeySetName());
    }

    @Test
    void definesDescriptionsForEveryBuiltInSetting() {
        for (Locale locale : List.of(Locale.US, Locale.SIMPLIFIED_CHINESE)) {
            assertFalse(ConfigJwtAccessTokenSettingsSource.definitions().stream()
                    .map(spec -> spec.getDescription().resolve(locale))
                    .anyMatch(String::isBlank));
        }
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
