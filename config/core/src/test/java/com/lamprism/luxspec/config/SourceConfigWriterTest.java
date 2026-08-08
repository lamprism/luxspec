package com.lamprism.luxspec.config;

import com.lamprism.luxspec.cache.CacheInvalidator;
import com.lamprism.luxspec.cache.CaffeineCaches;
import com.lamprism.luxspec.config.event.ConfigChangedEvent;
import com.lamprism.luxspec.config.event.ConfigSourceChangeType;
import com.lamprism.luxspec.config.event.ConfigSourceChangedEvent;
import com.lamprism.luxspec.config.policy.ConfigPolicies;
import com.lamprism.luxspec.config.policy.ConfigPolicyDecision;
import com.lamprism.luxspec.config.policy.ConfigPolicyException;
import com.lamprism.luxspec.config.policy.ConfigPolicyOperation;
import com.lamprism.luxspec.config.policy.ConfigSourceSelector;
import com.lamprism.luxspec.config.provider.CachingConfigProvider;
import com.lamprism.luxspec.config.resolution.ConfigValueOrigin;
import com.lamprism.luxspec.config.runtime.ConfigWriteException;
import com.lamprism.luxspec.config.runtime.LayeredConfigReader;
import com.lamprism.luxspec.config.runtime.SourceConfigWriter;
import com.lamprism.luxspec.config.source.ConfigEntry;
import com.lamprism.luxspec.config.source.ConfigSource;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.RawConfigValue;
import com.lamprism.luxspec.config.source.WritableConfigSource;
import com.lamprism.luxspec.config.value.ConfigValueValidationException;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SourceConfigWriterTest {
    @Test
    void publishesAnEventAfterASuccessfulWrite() {
        MutableSource source = new MutableSource();
        List<Object> events = new ArrayList<>();
        SourceConfigWriter writer = new SourceConfigWriter(List.of(source), events::add);
        ConfigSpec<String> spec = ConfigSpec.of("sample.value", ConfigCodecs.string(), null, false);

        writer.set(source.getId(), spec, "value");

        ConfigSourceChangedEvent event = (ConfigSourceChangedEvent) events.get(0);
        assertEquals(ConfigSourceChangeType.SET, event.getChangeType());
        assertEquals("value", source.value.requireString());
    }

    @Test
    void rejectsWritesBeforeCallingAReadOnlySource() {
        ConfigSource source = new ReadOnlySource();
        SourceConfigWriter writer = new SourceConfigWriter(List.of(source));
        ConfigSpec<String> spec = ConfigSpec.of("sample.value", ConfigCodecs.string(), null, false);

        assertThrows(UnsupportedOperationException.class, () -> writer.set(source.getId(), spec, "value"));
    }

    @Test
    void rejectsWritesToAnExactSourceIdThatDoesNotMatchTheSpec() {
        MutableSource source = new MutableSource();
        SourceConfigWriter writer = new SourceConfigWriter(List.of(source));
        ConfigSpec<String> spec = ConfigSpec.of(
                "sample.value",
                ConfigCodecs.string(),
                null,
                false,
                ConfigValueValidator.<String>none(),
                ConfigPolicies.sourceSelection(
                        Map.of(),
                        ConfigSourceSelector.exact(ConfigSourceId.of("global"))
                )
        );

        assertThrows(IllegalArgumentException.class, () -> writer.set(source.getId(), spec, "value"));
        assertNull(source.value);
    }

    @Test
    void rejectsAllMutationsForAReadOnlySpec() {
        MutableSource source = new MutableSource();
        SourceConfigWriter writer = new SourceConfigWriter(List.of(source));
        ConfigSpec<String> spec = ConfigSpec.builder("sample.value", ConfigCodecs.string())
                .policy(ConfigPolicies.readOnly())
                .build();

        assertThrows(ConfigPolicyException.class, () -> writer.set(source.getId(), spec, "value"));
        assertNull(source.value);
    }

    @Test
    void evaluatesRemoveAsItsOwnPolicyOperation() {
        MutableSource source = new MutableSource();
        SourceConfigWriter writer = new SourceConfigWriter(List.of(source));
        ConfigSpec<String> spec = ConfigSpec.builder("sample.value", ConfigCodecs.string())
                .policy(context -> context.getOperation() == ConfigPolicyOperation.REMOVE
                        ? ConfigPolicyDecision.DENY
                        : ConfigPolicyDecision.ALLOW)
                .build();

        assertThrows(ConfigPolicyException.class, () -> writer.remove(source.getId(), spec));
        assertNull(source.value);
    }

    @Test
    void rejectsAnInvalidValueBeforeMutatingTheSource() {
        MutableSource source = new MutableSource();
        SourceConfigWriter writer = new SourceConfigWriter(List.of(source));
        ConfigSpec<Integer> spec = ConfigSpec.of(
                "sample.limit",
                ConfigCodecs.integer(),
                null,
                false,
                ConfigValueValidator.of(value -> value > 0, "Value must be positive")
        );

        assertThrows(
                ConfigValueValidationException.class,
                () -> writer.set(source.getId(), spec, 0)
        );
        assertNull(source.value);
    }

    @Test
    void sanitizesValidatorFailuresBeforeTheyReachCallers() {
        MutableSource source = new MutableSource();
        SourceConfigWriter writer = new SourceConfigWriter(List.of(source));
        ConfigSpec<String> spec = ConfigSpec.builder("security.token", ConfigCodecs.string())
                .sensitive()
                .validator(value -> {
                    throw new IllegalArgumentException("secret-value");
                })
                .build();

        ConfigValueValidationException exception = assertThrows(
                ConfigValueValidationException.class,
                () -> writer.set(source.getId(), spec, "secret-value")
        );

        assertFalse(exception.getMessage().contains("secret-value"));
        assertNull(exception.getCause());
        assertNull(source.value);
    }

    @Test
    void sanitizesCodecFailuresBeforeTheyReachCallers() {
        MutableSource source = new MutableSource();
        SourceConfigWriter writer = new SourceConfigWriter(List.of(source));
        ConfigCodec<String> codec = new ConfigCodec<>() {
            @Override
            public String decode(@NonNull RawConfigValue rawValue) {
                return rawValue.requireString();
            }

            @Override
            public RawConfigValue encode(@NonNull String value) {
                throw new IllegalArgumentException("secret-value");
            }
        };
        ConfigSpec<String> spec = ConfigSpec.builder("security.token", codec)
                .sensitive()
                .build();

        ConfigValueValidationException exception = assertThrows(
                ConfigValueValidationException.class,
                () -> writer.set(source.getId(), spec, "secret-value")
        );

        assertFalse(exception.getMessage().contains("secret-value"));
        assertNull(exception.getCause());
        assertNull(source.value);
    }

    @Test
    void sanitizesSourceWriteFailuresBeforeTheyReachCallers() {
        FailingSource source = new FailingSource();
        SourceConfigWriter writer = new SourceConfigWriter(List.of(source));
        ConfigSpec<String> spec = ConfigSpec.builder("security.token", ConfigCodecs.string())
                .sensitive()
                .build();

        ConfigWriteException exception = assertThrows(
                ConfigWriteException.class,
                () -> writer.set(source.getId(), spec, "secret-value")
        );

        assertFalse(exception.getMessage().contains("secret-value"));
        assertNull(exception.getCause());
    }

    @Test
    void invalidatesTheEffectiveCacheAndPublishesValueFreeChangeMetadata() {
        MutableSource source = new MutableSource();
        ConfigSpec<String> spec = ConfigSpec.of("sample.value", ConfigCodecs.string(), "default", false);
        LayeredConfigReader layeredReader = new LayeredConfigReader(List.of(source));
        List<Object> events = new ArrayList<>();
        SourceConfigWriter sourceWriter = new SourceConfigWriter(
                List.of(source),
                source.getId(),
                events::add,
                layeredReader,
                CacheInvalidator.noOp()
        );
        CachingConfigProvider cachedReader = new CachingConfigProvider(
                ConfigProvider.of(layeredReader, sourceWriter),
                CaffeineCaches.create()
        );

        assertEquals("default", cachedReader.get(spec).getValue());
        cachedReader.set(spec, "updated");

        assertEquals("updated", cachedReader.get(spec).getValue());
        assertEquals(2, events.size());
        ConfigChangedEvent effectiveEvent = (ConfigChangedEvent) events.get(1);
        assertEquals(ConfigValueOrigin.DefaultOrigin.INSTANCE, effectiveEvent.getPreviousOrigin());
        assertEquals(new ConfigValueOrigin.SourceOrigin(source.getId()), effectiveEvent.getCurrentOrigin());
    }

    private static final class MutableSource implements WritableConfigSource {
        private final ConfigSourceId id = ConfigSourceId.of("memory");
        private RawConfigValue value;

        @Override
        public @NonNull ConfigSourceId getId() {
            return id;
        }

        @Override
        public @NonNull ConfigEntry get(@NonNull ConfigKey key) {
            return value == null ? ConfigEntry.absent() : ConfigEntry.present(value);
        }

        @Override
        public void set(@NonNull ConfigKey key, @NonNull RawConfigValue rawValue) {
            value = rawValue;
        }

        @Override
        public void remove(@NonNull ConfigKey key) {
            value = null;
        }
    }

    private static final class ReadOnlySource implements ConfigSource {
        private final ConfigSourceId id = ConfigSourceId.of("read-only");

        @Override
        public @NonNull ConfigSourceId getId() {
            return id;
        }

        @Override
        public @NonNull ConfigEntry get(@NonNull ConfigKey key) {
            return ConfigEntry.absent();
        }
    }

    private static final class FailingSource implements WritableConfigSource {
        private final ConfigSourceId id = ConfigSourceId.of("failing");

        @Override
        public @NonNull ConfigSourceId getId() {
            return id;
        }

        @Override
        public @NonNull ConfigEntry get(@NonNull ConfigKey key) {
            return ConfigEntry.absent();
        }

        @Override
        public void set(@NonNull ConfigKey key, @NonNull RawConfigValue rawValue) {
            throw new IllegalStateException("secret-value");
        }

        @Override
        public void remove(@NonNull ConfigKey key) {
            throw new IllegalStateException("secret-value");
        }
    }
}
