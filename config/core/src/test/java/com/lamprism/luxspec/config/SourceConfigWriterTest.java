package com.lamprism.luxspec.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SourceConfigWriterTest {
    @Test
    void publishesAnEventAfterASuccessfulWrite() {
        MutableSource source = new MutableSource(Set.of(ConfigSourceCapability.READ, ConfigSourceCapability.WRITE));
        List<Object> events = new ArrayList<>();
        SourceConfigWriter writer = new SourceConfigWriter(List.of(source), events::add);
        ConfigSpec<String> spec = FixedConfigSpec.of(ConfigKey.of("sample.value"), ConfigCodecs.string(), null, false);

        writer.set(source.getId(), spec, "value");

        ConfigSourceChangedEvent event = (ConfigSourceChangedEvent) events.get(0);
        assertEquals(ConfigSourceChangeType.SET, event.getChangeType());
        assertEquals("value", source.value.requireScalar());
    }

    @Test
    void rejectsWritesBeforeCallingASourceWithoutWriteCapability() {
        MutableSource source = new MutableSource(Set.of(ConfigSourceCapability.READ));
        SourceConfigWriter writer = new SourceConfigWriter(List.of(source));
        ConfigSpec<String> spec = FixedConfigSpec.of(ConfigKey.of("sample.value"), ConfigCodecs.string(), null, false);

        assertThrows(UnsupportedOperationException.class, () -> writer.set(source.getId(), spec, "value"));
        assertEquals(null, source.value);
    }

    @Test
    void rejectsWritesToAnExactSourceIdThatDoesNotMatchTheSpec() {
        MutableSource source = new MutableSource(Set.of(ConfigSourceCapability.READ, ConfigSourceCapability.WRITE));
        SourceConfigWriter writer = new SourceConfigWriter(List.of(source));
        ConfigSpec<String> spec = FixedConfigSpec.of(
                ConfigKey.of("sample.value"),
                ConfigCodecs.string(),
                null,
                false,
                Set.of(ConfigSourceCapability.READ),
                Map.of(),
                ConfigSourceId.of("global")
        );

        assertThrows(IllegalArgumentException.class, () -> writer.set(source.getId(), spec, "value"));
        assertEquals(null, source.value);
    }

    @Test
    void invalidatesTheEffectiveCacheAndPublishesValueFreeChangeMetadata() {
        MutableSource source = new MutableSource(Set.of(ConfigSourceCapability.READ, ConfigSourceCapability.WRITE));
        ConfigSpec<String> spec = FixedConfigSpec.of(ConfigKey.of("sample.value"), ConfigCodecs.string(), "default", false);
        LayeredConfigReader layeredReader = new LayeredConfigReader(List.of(new ConfigLayer(source)));
        CachingConfigReader cachedReader = new CachingConfigReader(layeredReader, key -> ConfigCachePolicy.invalidation());
        List<Object> events = new ArrayList<>();
        SourceConfigWriter writer = new SourceConfigWriter(
                List.of(source),
                source.getId(),
                events::add,
                cachedReader,
                cachedReader
        );

        assertEquals("default", cachedReader.get(spec).getValue().orElseThrow());
        writer.set(spec, "updated");

        assertEquals("updated", cachedReader.get(spec).getValue().orElseThrow());
        assertEquals(2, events.size());
        ConfigChangedEvent effectiveEvent = (ConfigChangedEvent) events.get(1);
        assertEquals(ResolvedConfig.Origin.DEFAULT, effectiveEvent.getPreviousOrigin());
        assertEquals(ResolvedConfig.Origin.SOURCE, effectiveEvent.getCurrentOrigin());
    }

    private static final class MutableSource implements ConfigSource {
        private final ConfigSourceId id = ConfigSourceId.of("memory");
        private final Set<ConfigSourceCapability> capabilities;
        private RawConfigValue value;

        private MutableSource(Set<ConfigSourceCapability> capabilities) {
            this.capabilities = capabilities;
        }

        @Override
        public ConfigSourceId getId() {
            return id;
        }

        @Override
        public Set<ConfigSourceCapability> getCapabilities() {
            return capabilities;
        }

        @Override
        public ConfigEntry get(ConfigKey key) {
            return value == null ? ConfigEntry.absent() : ConfigEntry.present(value);
        }

        @Override
        public void set(ConfigKey key, RawConfigValue rawValue) {
            value = rawValue;
        }
    }
}
