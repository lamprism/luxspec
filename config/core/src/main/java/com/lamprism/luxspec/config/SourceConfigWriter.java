package com.lamprism.luxspec.config;

import com.lamprism.luxspec.event.EventPublisher;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Writes configuration through sources registered by their stable instance ID.
 *
 * @author RollW
 */
public final class SourceConfigWriter implements ConfigWriter {
    private final Map<ConfigSourceId, ConfigSource> sources;
    private final @Nullable ConfigSourceId defaultSourceId;
    private final EventPublisher eventPublisher;
    private final @Nullable ConfigReader effectiveReader;
    private final ConfigCacheInvalidator cacheInvalidator;

    /**
     * Creates a writer that does not publish source change events.
     *
     * @param sources source instances indexed by stable IDs
     */
    public SourceConfigWriter(Iterable<? extends ConfigSource> sources) {
        this(sources, null, event -> { });
    }

    /**
     * Creates a writer that publishes a source change event after each completed mutation.
     *
     * @param sources source instances indexed by stable IDs
     * @param eventPublisher the event publisher to notify after mutation
     */
    public SourceConfigWriter(Iterable<? extends ConfigSource> sources, EventPublisher eventPublisher) {
        this(sources, null, eventPublisher);
    }

    /**
     * Creates a writer with one configured default target and no event publication.
     *
     * @param sources source instances indexed by stable IDs
     * @param defaultSourceId the optional target used by fluent write operations
     */
    public SourceConfigWriter(Iterable<? extends ConfigSource> sources, @Nullable ConfigSourceId defaultSourceId) {
        this(sources, defaultSourceId, event -> { });
    }

    /**
     * Creates a writer with an optional default target and source-change publication.
     *
     * @param sources source instances indexed by stable IDs
     * @param defaultSourceId the optional target used by fluent write operations
     * @param eventPublisher the event publisher to notify after mutation
     */
    public SourceConfigWriter(
            Iterable<? extends ConfigSource> sources,
            @Nullable ConfigSourceId defaultSourceId,
            EventPublisher eventPublisher
    ) {
        this(sources, defaultSourceId, eventPublisher, null, key -> { });
    }

    /**
     * Creates a writer that can invalidate cache entries and publish effective changes.
     *
     * @param sources source instances indexed by stable IDs
     * @param defaultSourceId the optional target used by fluent write operations
     * @param eventPublisher the event publisher to notify after mutation
     * @param effectiveReader the optional reader used to detect effective changes
     * @param cacheInvalidator the cache invalidator called after source change publication
     */
    public SourceConfigWriter(
            Iterable<? extends ConfigSource> sources,
            @Nullable ConfigSourceId defaultSourceId,
            EventPublisher eventPublisher,
            @Nullable ConfigReader effectiveReader,
            ConfigCacheInvalidator cacheInvalidator
    ) {
        Map<ConfigSourceId, ConfigSource> indexed = new LinkedHashMap<>();
        for (ConfigSource source : sources) {
            ConfigSource nonNullSource = Objects.requireNonNull(source, "source");
            if (indexed.put(nonNullSource.getId(), nonNullSource) != null) {
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
    public <T> void set(ConfigSpec<T> spec, T value) {
        set(defaultSource(), spec, value);
    }

    @Override
    public <T> void set(ConfigSourceId sourceId, ConfigSpec<T> spec, T value) {
        ConfigSource source = source(sourceId);
        ConfigSpec<T> nonNullSpec = Objects.requireNonNull(spec, "spec");
        requireCapabilities(source, nonNullSpec, ConfigSourceCapability.WRITE);
        ResolvedConfig<?> previous = readEffective(nonNullSpec);
        source.set(nonNullSpec.getKey(), nonNullSpec.getCodec().encode(Objects.requireNonNull(value, "value")));
        complete(source, nonNullSpec, previous, ConfigSourceChangeType.SET);
    }

    @Override
    public void remove(ConfigSpec<?> spec) {
        remove(defaultSource(), spec);
    }

    @Override
    public void remove(ConfigSourceId sourceId, ConfigSpec<?> spec) {
        ConfigSource source = source(sourceId);
        ConfigSpec<?> nonNullSpec = Objects.requireNonNull(spec, "spec");
        requireCapabilities(source, nonNullSpec, ConfigSourceCapability.WRITE);
        ResolvedConfig<?> previous = readEffective(nonNullSpec);
        source.remove(nonNullSpec.getKey());
        complete(source, nonNullSpec, previous, ConfigSourceChangeType.REMOVE);
    }

    @Override
    public void mask(ConfigSpec<?> spec) {
        mask(defaultSource(), spec);
    }

    @Override
    public void mask(ConfigSourceId sourceId, ConfigSpec<?> spec) {
        ConfigSource source = source(sourceId);
        ConfigSpec<?> nonNullSpec = Objects.requireNonNull(spec, "spec");
        requireCapabilities(source, nonNullSpec, ConfigSourceCapability.MASK);
        ResolvedConfig<?> previous = readEffective(nonNullSpec);
        source.mask(nonNullSpec.getKey());
        complete(source, nonNullSpec, previous, ConfigSourceChangeType.MASK);
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

    private void requireCapabilities(
            ConfigSource source,
            ConfigSpec<?> spec,
            ConfigSourceCapability operationCapability
    ) {
        if (!source.getCapabilities().contains(operationCapability)) {
            throw new UnsupportedOperationException("Configuration source does not support required capability");
        }
        if (!ConfigSourceConstraints.matches(source, spec)) {
            throw new IllegalArgumentException("Configuration source does not satisfy specification requirements");
        }
    }

    private <T> @Nullable ResolvedConfig<T> readEffective(ConfigSpec<T> spec) {
        if (effectiveReader == null) {
            return null;
        }
        return effectiveReader.get(spec, ConfigReadOption.FRESH);
    }

    private void complete(
            ConfigSource source,
            ConfigSpec<?> spec,
            @Nullable ResolvedConfig<?> previous,
            ConfigSourceChangeType changeType
    ) {
        eventPublisher.publish(new ConfigSourceChangedEvent(source.getId(), spec.getKey(), changeType));
        cacheInvalidator.invalidate(spec.getKey());
        if (previous == null) {
            return;
        }
        ResolvedConfig<?> current = effectiveReader.get(spec, ConfigReadOption.FRESH);
        if (!previous.equals(current)) {
            eventPublisher.publish(new ConfigChangedEvent(spec, previous, current));
        }
    }
}
