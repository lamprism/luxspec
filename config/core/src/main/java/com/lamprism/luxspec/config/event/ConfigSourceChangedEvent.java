package com.lamprism.luxspec.config.event;

import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.source.ConfigSourceId;

import java.util.Objects;

/**
 * Reports a completed source mutation without exposing a raw configuration value.
 *
 * @author RollW
 */
public final class ConfigSourceChangedEvent implements ConfigEvent {
    private final ConfigSourceId sourceId;
    private final ConfigKey key;
    private final ConfigSourceChangeType changeType;

    /**
     * Creates a source mutation event.
     *
     * @param sourceId   the source that completed the mutation
     * @param key        the affected configuration key
     * @param changeType the completed mutation type
     */
    public ConfigSourceChangedEvent(ConfigSourceId sourceId, ConfigKey key, ConfigSourceChangeType changeType) {
        this.sourceId = Objects.requireNonNull(sourceId, "sourceId");
        this.key = Objects.requireNonNull(key, "key");
        this.changeType = Objects.requireNonNull(changeType, "changeType");
    }

    /**
     * Returns the source that completed the mutation.
     *
     * @return the source identifier
     */
    public ConfigSourceId getSourceId() {
        return sourceId;
    }

    /**
     * Returns the affected complete configuration key.
     *
     * @return the configuration key
     */
    public ConfigKey getKey() {
        return key;
    }

    @Override
    public Kind getKind() {
        return Kind.SOURCE_CHANGED;
    }

    /**
     * Returns the completed raw mutation type.
     *
     * @return the mutation type
     */
    public ConfigSourceChangeType getChangeType() {
        return changeType;
    }
}
