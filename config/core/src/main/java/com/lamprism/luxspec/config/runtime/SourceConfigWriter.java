package com.lamprism.luxspec.config.runtime;

import com.lamprism.luxspec.cache.CacheInvalidator;
import com.lamprism.luxspec.config.ConfigBinding;
import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigReader;
import com.lamprism.luxspec.config.ConfigValue;
import com.lamprism.luxspec.config.ConfigWriter;
import com.lamprism.luxspec.config.cache.FreshConfigReader;
import com.lamprism.luxspec.config.event.ConfigChangedEvent;
import com.lamprism.luxspec.config.event.ConfigSourceChangeType;
import com.lamprism.luxspec.config.event.ConfigSourceChangedEvent;
import com.lamprism.luxspec.config.policy.ConfigPolicyContext;
import com.lamprism.luxspec.config.policy.ConfigPolicyDecision;
import com.lamprism.luxspec.config.policy.ConfigPolicyException;
import com.lamprism.luxspec.config.policy.ConfigPolicyOperation;
import com.lamprism.luxspec.config.source.ConfigSource;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.RawConfigValue;
import com.lamprism.luxspec.config.source.WritableConfigSource;
import com.lamprism.luxspec.config.value.ConfigValueValidationException;
import com.lamprism.luxspec.event.EventPublisher;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Writes configuration through sources registered by their stable instance ID.
 *
 * @author RollW
 */
public class SourceConfigWriter implements ConfigWriter {
    private final Map<ConfigSourceId, ConfigSource> sources;
    private final @Nullable ConfigSourceId defaultSourceId;
    private final EventPublisher eventPublisher;
    private final @Nullable ConfigReader effectiveReader;
    private final CacheInvalidator<ConfigKey> cacheInvalidator;

    /**
     * Creates a writer with no default source and a no-op event publisher.
     *
     * @param sources the configured source instances
     */
    public SourceConfigWriter(Iterable<? extends ConfigSource> sources) {
        this(sources, null, event -> {
        });
    }

    /**
     * Creates a writer with no default source.
     *
     * @param sources        the configured source instances
     * @param eventPublisher the publisher for source mutation events
     */
    public SourceConfigWriter(Iterable<? extends ConfigSource> sources, EventPublisher eventPublisher) {
        this(sources, null, eventPublisher);
    }

    /**
     * Creates a writer with a default source and a no-op event publisher.
     *
     * @param sources         the configured source instances
     * @param defaultSourceId the source used by operations without an explicit source ID
     */
    public SourceConfigWriter(Iterable<? extends ConfigSource> sources, @Nullable ConfigSourceId defaultSourceId) {
        this(sources, defaultSourceId, event -> {
        });
    }

    /**
     * Creates a writer with a default source and event publisher.
     *
     * @param sources         the configured source instances
     * @param defaultSourceId the source used by operations without an explicit source ID
     * @param eventPublisher  the publisher for source mutation events
     */
    public SourceConfigWriter(
            Iterable<? extends ConfigSource> sources,
            @Nullable ConfigSourceId defaultSourceId,
            EventPublisher eventPublisher
    ) {
        this(sources, defaultSourceId, eventPublisher, null, CacheInvalidator.noOp());
    }

    /**
     * Creates a writer with optional effective-state observation and cache invalidation.
     *
     * @param sources          the configured source instances
     * @param defaultSourceId  the source used by operations without an explicit source ID
     * @param eventPublisher   the publisher for source mutation events
     * @param effectiveReader  the reader used to compare effective state changes
     * @param cacheInvalidator the cache invalidator called after source mutations
     */
    public SourceConfigWriter(
            Iterable<? extends ConfigSource> sources,
            @Nullable ConfigSourceId defaultSourceId,
            EventPublisher eventPublisher,
            @Nullable ConfigReader effectiveReader,
            CacheInvalidator<ConfigKey> cacheInvalidator
    ) {
        Map<ConfigSourceId, ConfigSource> indexed = new LinkedHashMap<>();
        for (ConfigSource source : Objects.requireNonNull(sources, "sources")) {
            ConfigSource nonNullSource = Objects.requireNonNull(source, "source");
            ConfigSourceId sourceId = Objects.requireNonNull(nonNullSource.getId(), "source ID");
            if (indexed.put(sourceId, nonNullSource) != null) {
                throw new IllegalArgumentException("Duplicate configuration source ID");
            }
        }
        this.sources = Map.copyOf(indexed);
        if (defaultSourceId != null && !this.sources.containsKey(defaultSourceId)) {
            throw new IllegalArgumentException("Default configuration source ID is unknown");
        }
        this.defaultSourceId = defaultSourceId;
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.effectiveReader = effectiveReader;
        this.cacheInvalidator = Objects.requireNonNull(cacheInvalidator, "cacheInvalidator");
    }

    @Override
    public <T> void set(ConfigBinding<T> binding, T value) {
        set(defaultSource(), binding, value);
    }

    @Override
    public <T> void set(ConfigSourceId sourceId, ConfigBinding<T> binding, T value) {
        ConfigBinding<T> nonNullBinding = Objects.requireNonNull(binding, "binding");
        ConfigSource source = source(sourceId);
        requirePolicy(source, nonNullBinding, ConfigPolicyOperation.SET);
        T nonNullValue = Objects.requireNonNull(value, "value");
        RawConfigValue rawValue = encode(nonNullBinding, nonNullValue);
        ConfigValue<?> previous = readEffective(nonNullBinding);
        WritableConfigSource writableSource = writable(source);
        try {
            writableSource.set(nonNullBinding.getKey(), rawValue);
        } catch (RuntimeException exception) {
            throw new ConfigWriteException(
                    nonNullBinding.getKey(),
                    source.getId(),
                    ConfigPolicyOperation.SET,
                    exception
            );
        }
        complete(source, nonNullBinding, previous, ConfigSourceChangeType.SET);
    }

    @Override
    public void remove(ConfigBinding<?> binding) {
        remove(defaultSource(), binding);
    }

    @Override
    public void remove(ConfigSourceId sourceId, ConfigBinding<?> binding) {
        ConfigBinding<?> nonNullBinding = Objects.requireNonNull(binding, "binding");
        ConfigSource source = source(sourceId);
        requirePolicy(source, nonNullBinding, ConfigPolicyOperation.REMOVE);
        ConfigValue<?> previous = readEffective(nonNullBinding);
        WritableConfigSource writableSource = writable(source);
        try {
            writableSource.remove(nonNullBinding.getKey());
        } catch (RuntimeException exception) {
            throw new ConfigWriteException(
                    nonNullBinding.getKey(),
                    source.getId(),
                    ConfigPolicyOperation.REMOVE,
                    exception
            );
        }
        complete(source, nonNullBinding, previous, ConfigSourceChangeType.REMOVE);
    }

    private ConfigSource source(ConfigSourceId sourceId) {
        ConfigSource source = sources.get(Objects.requireNonNull(sourceId, "sourceId"));
        if (source == null) {
            throw new IllegalArgumentException("Unknown configuration source ID");
        }
        return source;
    }

    private ConfigSourceId defaultSource() {
        if (defaultSourceId == null) {
            throw new IllegalStateException("No default configuration source is configured");
        }
        return defaultSourceId;
    }

    private void requirePolicy(
            ConfigSource source,
            ConfigBinding<?> binding,
            ConfigPolicyOperation policyOperation
    ) {
        ConfigPolicyDecision decision = Objects.requireNonNull(
                binding.getSpec().getPolicy().evaluate(
                        ConfigPolicyContext.forSourceOperation(policyOperation, binding, source)
                ),
                "policy decision"
        );
        if (decision != ConfigPolicyDecision.ALLOW) {
            throw new ConfigPolicyException(binding.getKey(), policyOperation, decision);
        }
    }

    private WritableConfigSource writable(ConfigSource source) {
        if (!(source instanceof WritableConfigSource writableSource)) {
            throw new UnsupportedOperationException("Configuration source is not writable");
        }
        return writableSource;
    }

    private static <T> RawConfigValue encode(ConfigBinding<T> binding, T value) {
        try {
            binding.getSpec().validate(value);
        } catch (RuntimeException exception) {
            throw new ConfigValueValidationException("Configuration value failed validation");
        }
        try {
            return binding.getSpec().getCodec().encode(value);
        } catch (RuntimeException exception) {
            throw new ConfigValueValidationException("Configuration value could not be encoded");
        }
    }

    private <T> @Nullable ConfigValue<T> readEffective(ConfigBinding<T> binding) {
        if (effectiveReader == null) {
            return null;
        }
        if (effectiveReader instanceof FreshConfigReader freshReader) {
            return freshReader.getFresh(binding);
        }
        return effectiveReader.get(binding);
    }

    private void complete(
            ConfigSource source,
            ConfigBinding<?> binding,
            @Nullable ConfigValue<?> previous,
            ConfigSourceChangeType changeType
    ) {
        cacheInvalidator.invalidate(binding.getKey());
        eventPublisher.publish(new ConfigSourceChangedEvent(source.getId(), binding.getKey(), changeType));
        if (previous == null || effectiveReader == null) {
            return;
        }
        ConfigValue<?> current = readEffective(binding);
        if (!previous.equals(current)) {
            eventPublisher.publish(new ConfigChangedEvent(binding, previous, current));
        }
    }
}
