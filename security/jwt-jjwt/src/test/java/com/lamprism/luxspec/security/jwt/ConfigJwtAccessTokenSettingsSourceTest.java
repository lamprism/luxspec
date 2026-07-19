package com.lamprism.luxspec.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lamprism.luxspec.config.ConfigEntry;
import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigLayer;
import com.lamprism.luxspec.config.ConfigSource;
import com.lamprism.luxspec.config.ConfigSourceCapability;
import com.lamprism.luxspec.config.ConfigSourceId;
import com.lamprism.luxspec.config.LayeredConfigReader;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConfigJwtAccessTokenSettingsSourceTest {
    @Test
    void loadsTypedAccessTokenSettingsAndKeySetReference() {
        Map<ConfigKey, ConfigEntry> entries = new HashMap<>();
        entries.put(JwtAccessTokenConfigSpecs.ACCESS_TTL.getKey(), ConfigEntry.present("PT10M"));
        entries.put(JwtAccessTokenConfigSpecs.ACCESS_ISSUER.getKey(), ConfigEntry.present("luxspec-test"));
        entries.put(JwtAccessTokenConfigSpecs.ACCESS_AUDIENCES.getKey(), ConfigEntry.present(List.of("api")));
        entries.put(JwtAccessTokenConfigSpecs.KEY_SET_NAME.getKey(), ConfigEntry.present("access"));
        LayeredConfigReader reader = new LayeredConfigReader(List.of(new ConfigLayer(new MemorySource(entries))));
        ConfigJwtAccessTokenSettingsSource source = new ConfigJwtAccessTokenSettingsSource(reader);

        JwtAccessTokenOptions options = source.getOptions();

        assertEquals(Duration.ofMinutes(10), options.getLifetime());
        assertEquals("luxspec-test", options.getIssuer());
        assertEquals(Set.of("api"), options.getAudiences());
        assertEquals("access", source.getKeySetName());
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
