package com.lamprism.luxspec.config;

import com.lamprism.luxspec.config.policy.ConfigPolicies;
import com.lamprism.luxspec.config.policy.ConfigPolicyDecision;
import com.lamprism.luxspec.config.policy.ConfigPolicyException;
import com.lamprism.luxspec.config.policy.ConfigPolicyOperation;
import com.lamprism.luxspec.config.policy.ConfigPolicyPhase;
import com.lamprism.luxspec.config.policy.ConfigSourceSelector;
import com.lamprism.luxspec.config.resolution.ConfigResolutionException;
import com.lamprism.luxspec.config.resolution.ConfigValueOrigin;
import com.lamprism.luxspec.config.resolution.LayeredConfigValue;
import com.lamprism.luxspec.config.runtime.LayeredConfigReader;
import com.lamprism.luxspec.config.source.ConfigEntry;
import com.lamprism.luxspec.config.source.ConfigSource;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.ConfigSourceScope;
import com.lamprism.luxspec.validation.Validator;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LayeredConfigReaderTest {
    @Test
    void exposesSourceReadPhasesToPolicies() {
        List<ConfigPolicyPhase> phases = new ArrayList<>();
        ConfigSpec<String> spec = ConfigSpec.builder("sample.value", ConfigCodecs.string())
                .policy(context -> {
                    phases.add(context.getPhase());
                    return ConfigPolicyDecision.ALLOW;
                })
                .build();

        new LayeredConfigReader(List.of(
                new MemorySource("memory", ConfigEntry.present("value"))
        )).get(spec);

        assertEquals(
                List.of(
                        ConfigPolicyPhase.BEFORE_SOURCE_READ,
                        ConfigPolicyPhase.AFTER_SOURCE_READ,
                        ConfigPolicyPhase.OPERATION
                ),
                phases
        );
    }

    @Test
    void usesTheHighestPresentLayer() {
        ConfigSpec<Integer> spec = ConfigSpec.of(
                "sample.limit",
                ConfigCodecs.integer(),
                10,
                false
        );
        MemorySource higher = new MemorySource("higher", ConfigEntry.present("20"));
        MemorySource lower = new MemorySource("lower", ConfigEntry.present("30"));

        LayeredConfigValue<Integer> resolved = new LayeredConfigReader(List.of(higher, lower)).get(spec);

        assertEquals(20, resolved.getValue());
        assertEquals(new ConfigValueOrigin.SourceOrigin(ConfigSourceId.of("higher")), resolved.getOrigin());
    }

    @Test
    void tombstoneEntryDoesNotFallBackToALowerLayerOrDefault() {
        ConfigSpec<Integer> spec = ConfigSpec.of(
                "sample.limit",
                ConfigCodecs.integer(),
                10,
                false
        );
        MemorySource higher = new MemorySource("higher", ConfigEntry.tombstone());
        MemorySource lower = new MemorySource("lower", ConfigEntry.present("30"));

        LayeredConfigValue<Integer> resolved = new LayeredConfigReader(List.of(higher, lower)).get(spec);

        assertEquals(new ConfigValueOrigin.TombstoneOrigin(ConfigSourceId.of("higher")), resolved.getOrigin());
        assertNull(resolved.getValue());
    }

    @Test
    void invalidHigherValueDoesNotFallBackToALowerLayer() {
        ConfigSpec<Integer> spec = ConfigSpec.of(
                "sample.limit",
                ConfigCodecs.integer(),
                10,
                false
        );
        MemorySource higher = new MemorySource("higher", ConfigEntry.present("invalid"));
        MemorySource lower = new MemorySource("lower", ConfigEntry.present("30"));

        assertThrows(
                ConfigResolutionException.class,
                () -> new LayeredConfigReader(List.of(higher, lower)).get(spec)
        );
    }

    @Test
    void invalidLowerValueFailsBecauseTheReaderObservesEveryParticipatingLayer() {
        ConfigSpec<Integer> spec = ConfigSpec.of(
                "sample.limit",
                ConfigCodecs.integer(),
                10,
                false
        );
        MemorySource higher = new MemorySource("higher", ConfigEntry.present("20"));
        MemorySource lower = new MemorySource("lower", ConfigEntry.present("invalid"));

        assertThrows(
                ConfigResolutionException.class,
                () -> new LayeredConfigReader(List.of(higher, lower)).get(spec)
        );
    }

    @Test
    void rejectsAValueThatDecodesButViolatesTheDefinition() {
        ConfigSpec<Integer> spec = ConfigSpec.of(
                "sample.limit",
                ConfigCodecs.integer(),
                10,
                false,
                Validator.of(value -> value > 0, "Value must be positive")
        );
        MemorySource higher = new MemorySource("higher", ConfigEntry.present("0"));
        MemorySource lower = new MemorySource("lower", ConfigEntry.present("30"));

        ConfigResolutionException exception = assertThrows(
                ConfigResolutionException.class,
                () -> new LayeredConfigReader(List.of(higher, lower)).get(spec)
        );

        assertEquals(ConfigErrorCode.INVALID_VALUE, exception.getErrorCode());
        assertEquals("Configuration value is invalid", exception.getMessage());
        assertFalse(exception.getMessage().contains("sample.limit"));
        assertFalse(exception.getMessage().contains("higher"));
        assertFalse(exception.getMessage().contains("Value must be positive"));
        assertNull(exception.getCause());
    }

    @Test
    void sourceReportedInvalidEntryDoesNotFallBackToALowerLayer() {
        ConfigSpec<Integer> spec = ConfigSpec.of(
                "sample.limit",
                ConfigCodecs.integer(),
                10,
                false
        );
        MemorySource higher = new MemorySource("higher", ConfigEntry.invalid("Source document cannot be read"));
        MemorySource lower = new MemorySource("lower", ConfigEntry.present("30"));

        assertThrows(
                ConfigResolutionException.class,
                () -> new LayeredConfigReader(List.of(higher, lower)).get(spec)
        );
    }

    @Test
    void resolvesOnlyFromASourceThatMatchesAttributesAndExactId() {
        ConfigSpec<Integer> spec = ConfigSpec.of(
                "sample.limit",
                ConfigCodecs.integer(),
                10,
                false,
                Validator.<Integer>none(),
                ConfigPolicies.sourceSelection(
                        Map.of("tier", "database"),
                        ConfigSourceSelector.exact(ConfigSourceId.of("global"))
                )
        );
        MemorySource wrongId = new MemorySource("tenant", ConfigEntry.present("20"), Map.of("tier", "database"));
        MemorySource matching = new MemorySource("global", ConfigEntry.present("30"), Map.of("tier", "database"));

        LayeredConfigValue<Integer> resolved = new LayeredConfigReader(List.of(wrongId, matching)).get(spec);

        assertEquals(30, resolved.getValue());
        assertEquals(new ConfigValueOrigin.SourceOrigin(ConfigSourceId.of("global")), resolved.getOrigin());
    }

    @Test
    void usesTheSpecDefaultWhenNoSourceMatchesItsAttributes() {
        ConfigSpec<Integer> spec = ConfigSpec.of(
                "sample.limit",
                ConfigCodecs.integer(),
                10,
                false,
                Validator.<Integer>none(),
                ConfigPolicies.sourceSelection(
                        Map.of("tier", "database"),
                        ConfigSourceSelector.any()
                )
        );
        MemorySource source = new MemorySource(
                "environment",
                ConfigEntry.present("20"),
                Map.of("tier", "runtime")
        );

        LayeredConfigValue<Integer> resolved = new LayeredConfigReader(List.of(source)).get(spec);

        assertEquals(10, resolved.getValue());
        assertEquals(ConfigValueOrigin.DefaultOrigin.INSTANCE, resolved.getOrigin());
    }

    @Test
    void doesNotUseTheSpecDefaultWhenFallbackIsStoppedByPolicy() {
        ConfigSpec<Integer> spec = ConfigSpec.builder("sample.limit", ConfigCodecs.integer())
                .defaultValue(10)
                .policy(ConfigPolicies.noFallback())
                .build();

        LayeredConfigValue<Integer> resolved = new LayeredConfigReader(List.of(
                new MemorySource("environment", ConfigEntry.absent())
        )).get(spec);

        assertEquals(ConfigValue.State.ABSENT, resolved.getState());
        assertNull(resolved.getValue());
    }

    @Test
    void rejectsSkipWhenAFallbackCandidateIsBeingEvaluated() {
        ConfigSpec<Integer> spec = ConfigSpec.builder("sample.limit", ConfigCodecs.integer())
                .defaultValue(10)
                .policy(context -> context.getOperation() == ConfigPolicyOperation.RESOLVE_FALLBACK
                        ? ConfigPolicyDecision.SKIP
                        : ConfigPolicyDecision.ALLOW)
                .build();

        assertThrows(
                ConfigPolicyException.class,
                () -> new LayeredConfigReader(List.of(
                        new MemorySource("environment", ConfigEntry.absent())
                )).get(spec)
        );
    }

    @Test
    void sensitiveDefinitionsMayUseAReadOnlySourceWithoutImplicitSecureSelection() {
        ConfigSpec<String> spec = ConfigSpec.builder("security.token", ConfigCodecs.string())
                .sensitive()
                .build();

        LayeredConfigValue<String> resolved = new LayeredConfigReader(List.of(
                new MemorySource("environment", ConfigEntry.present("token"))
        )).get(spec);

        assertEquals("token", resolved.getValue());
    }

    @Test
    void appliesSourceRequirementsToParameterizedBindings() {
        ConfigSpec<Integer> spec = ConfigSpec.of(
                "tenant.{name}.limit",
                List.of(new ConfigParameter("name", Set.of("acme"))),
                ConfigCodecs.integer(),
                10,
                false,
                Validator.<Integer>none(),
                ConfigPolicies.sourceSelection(
                        Map.of("tier", "database"),
                        ConfigSourceSelector.exact(ConfigSourceId.of("global"))
                )
        );
        MemorySource wrong = new MemorySource("tenant", ConfigEntry.present("20"), Map.of("tier", "database"));
        MemorySource matching = new MemorySource("global", ConfigEntry.present("30"), Map.of("tier", "database"));

        LayeredConfigValue<Integer> resolved = new LayeredConfigReader(List.of(wrong, matching)).get(
                spec,
                Map.of("name", "acme")
        );

        assertEquals(30, resolved.getValue());
        assertEquals(new ConfigValueOrigin.SourceOrigin(ConfigSourceId.of("global")), resolved.getOrigin());
    }

    @Test
    void exposesEveryParticipatingLayerThroughTheCommonResult() {
        ConfigSpec<Integer> spec = ConfigSpec.of(
                "sample.limit",
                ConfigCodecs.integer(),
                10,
                false
        );
        LayeredConfigReader reader = new LayeredConfigReader(List.of(
                new MemorySource("higher", ConfigEntry.present("20")),
                new MemorySource("lower", ConfigEntry.absent())
        ));

        LayeredConfigValue<Integer> resolved = reader.get(spec.bind());
        List<ConfigValue<Integer>> values = resolved.getLayers();

        assertEquals(3, values.size());
        assertEquals(ConfigValue.State.PRESENT, values.get(0).getState());
        assertEquals(20, values.get(0).getValue());
        assertEquals(ConfigValue.State.ABSENT, values.get(1).getState());
        assertNull(values.get(1).getValue());
        assertEquals(ConfigValue.State.PRESENT, values.get(2).getState());
        assertEquals(ConfigValueOrigin.DefaultOrigin.INSTANCE, values.get(2).getOrigin());
    }

    @Test
    void skipsSourcesOutsideTheSpecScope() {
        ConfigSpec<String> spec = ConfigSpec.builder("database.type", ConfigCodecs.string())
                .policy(ConfigPolicies.sourceScope(ConfigSourceScope.BOOTSTRAP))
                .build();

        ConfigValue<String> value = new LayeredConfigReader(List.of(
                new MemorySource("runtime", ConfigEntry.present("postgresql"))
        )).get(spec);

        assertEquals(ConfigValue.State.ABSENT, value.getState());
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
        public ConfigSourceScope getScope() {
            return ConfigSourceScope.RUNTIME;
        }

        @Override
        public Map<String, String> getAttributes() {
            return attributes;
        }

        @Override
        public ConfigEntry get(@NonNull ConfigKey key) {
            return entry;
        }
    }
}
