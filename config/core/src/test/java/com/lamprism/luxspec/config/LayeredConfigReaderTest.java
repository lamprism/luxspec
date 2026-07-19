package com.lamprism.luxspec.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LayeredConfigReaderTest {
    @Test
    void usesTheHighestPresentLayer() {
        ConfigKey key = ConfigKey.of("sample.limit");
        ConfigSpec<Integer> spec = FixedConfigSpec.of(key, ConfigCodecs.integer(), 10, false);
        MemorySource higher = new MemorySource("higher", ConfigEntry.present("20"));
        MemorySource lower = new MemorySource("lower", ConfigEntry.present("30"));

        ResolvedConfig<Integer> resolved = new LayeredConfigReader(List.of(new ConfigLayer(higher), new ConfigLayer(lower))).get(spec);

        assertEquals(20, resolved.getValue().orElseThrow());
        assertEquals(ResolvedConfig.Origin.SOURCE, resolved.getOrigin());
        assertEquals(ConfigSourceId.of("higher"), resolved.getSourceId().orElseThrow());
    }

    @Test
    void maskDoesNotFallBackToALowerLayerOrDefault() {
        ConfigKey key = ConfigKey.of("sample.limit");
        ConfigSpec<Integer> spec = FixedConfigSpec.of(key, ConfigCodecs.integer(), 10, false);
        MemorySource higher = new MemorySource("higher", ConfigEntry.masked());
        MemorySource lower = new MemorySource("lower", ConfigEntry.present("30"));

        ResolvedConfig<Integer> resolved = new LayeredConfigReader(List.of(new ConfigLayer(higher), new ConfigLayer(lower))).get(spec);

        assertEquals(ResolvedConfig.Origin.MASKED, resolved.getOrigin());
        assertEquals(true, resolved.getValue().isEmpty());
    }

    @Test
    void invalidHigherValueDoesNotFallBackToALowerLayer() {
        ConfigKey key = ConfigKey.of("sample.limit");
        ConfigSpec<Integer> spec = FixedConfigSpec.of(key, ConfigCodecs.integer(), 10, false);
        MemorySource higher = new MemorySource("higher", ConfigEntry.present("invalid"));
        MemorySource lower = new MemorySource("lower", ConfigEntry.present("30"));

        assertThrows(
                ConfigResolutionException.class,
                () -> new LayeredConfigReader(List.of(new ConfigLayer(higher), new ConfigLayer(lower))).get(spec)
        );
    }

    @Test
    void sourceReportedInvalidEntryDoesNotFallBackToALowerLayer() {
        ConfigKey key = ConfigKey.of("sample.limit");
        ConfigSpec<Integer> spec = FixedConfigSpec.of(key, ConfigCodecs.integer(), 10, false);
        MemorySource higher = new MemorySource("higher", ConfigEntry.invalid("Source document cannot be read"));
        MemorySource lower = new MemorySource("lower", ConfigEntry.present("30"));

        assertThrows(
                ConfigResolutionException.class,
                () -> new LayeredConfigReader(List.of(new ConfigLayer(higher), new ConfigLayer(lower))).get(spec)
        );
    }

    @Test
    void resolvesOnlyFromASourceThatMatchesAttributesAndExactId() {
        ConfigKey key = ConfigKey.of("sample.limit");
        ConfigSpec<Integer> spec = FixedConfigSpec.of(
                key,
                ConfigCodecs.integer(),
                10,
                false,
                Set.of(ConfigSourceCapability.READ),
                Map.of("tier", "database"),
                ConfigSourceId.of("global")
        );
        MemorySource wrongId = new MemorySource("tenant", ConfigEntry.present("20"), Map.of("tier", "database"));
        MemorySource matching = new MemorySource("global", ConfigEntry.present("30"), Map.of("tier", "database"));

        ResolvedConfig<Integer> resolved = new LayeredConfigReader(
                List.of(new ConfigLayer(wrongId), new ConfigLayer(matching))
        ).get(spec);

        assertEquals(30, resolved.getValue().orElseThrow());
        assertEquals(ConfigSourceId.of("global"), resolved.getSourceId().orElseThrow());
    }

    @Test
    void usesTheSpecDefaultWhenNoSourceMatchesItsAttributes() {
        ConfigSpec<Integer> spec = FixedConfigSpec.of(
                ConfigKey.of("sample.limit"),
                ConfigCodecs.integer(),
                10,
                false,
                Set.of(ConfigSourceCapability.READ),
                Map.of("tier", "database"),
                null
        );
        MemorySource source = new MemorySource(
                "environment",
                ConfigEntry.present("20"),
                Map.of("tier", "runtime")
        );

        ResolvedConfig<Integer> resolved = new LayeredConfigReader(List.of(new ConfigLayer(source))).get(spec);

        assertEquals(10, resolved.getValue().orElseThrow());
        assertEquals(ResolvedConfig.Origin.DEFAULT, resolved.getOrigin());
    }

    @Test
    void appliesSourceRequirementsToBoundTemplates() {
        TemplateConfigSpec<Integer> template = TemplateConfigSpec.of(
                "tenant.{name}.limit",
                List.of(new ConfigParameter("name", Set.of("acme"))),
                ConfigCodecs.integer(),
                10,
                false,
                Set.of(ConfigSourceCapability.READ),
                Map.of("tier", "database"),
                ConfigSourceId.of("global")
        );
        MemorySource wrong = new MemorySource("tenant", ConfigEntry.present("20"), Map.of("tier", "database"));
        MemorySource matching = new MemorySource("global", ConfigEntry.present("30"), Map.of("tier", "database"));

        ResolvedConfig<Integer> resolved = new LayeredConfigReader(
                List.of(new ConfigLayer(wrong), new ConfigLayer(matching))
        ).get(template, Map.of("name", "acme"));

        assertEquals(30, resolved.getValue().orElseThrow());
        assertEquals(ConfigSourceId.of("global"), resolved.getSourceId().orElseThrow());
    }

    private static final class MemorySource implements ConfigSource {
        private final ConfigSourceId id;
        private final ConfigEntry entry;
        private final Map<String, String> attributes;

        private MemorySource(String id, ConfigEntry entry) {
            this(id, entry, Map.of());
        }

        private MemorySource(String id, ConfigEntry entry, Map<String, String> attributes) {
            this.id = ConfigSourceId.of(id);
            this.entry = entry;
            this.attributes = Map.copyOf(attributes);
        }

        @Override
        public ConfigSourceId getId() {
            return id;
        }

        @Override
        public Set<ConfigSourceCapability> getCapabilities() {
            return Set.of(ConfigSourceCapability.READ);
        }

        @Override
        public Map<String, String> getAttributes() {
            return attributes;
        }

        @Override
        public ConfigEntry get(ConfigKey key) {
            return entry;
        }
    }
}
