package com.lamprism.luxspec.config.persistence;

import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Identifies one configuration entry within one persistent configuration source.
 *
 * @author RollW
 */
@Embeddable
public class JpaConfigEntryId implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    static final int SOURCE_ID_LENGTH = 128;
    static final int CONFIG_KEY_LENGTH = 512;

    /**
     * Stores the persistent source partition.
     */
    @Column(name = "source_id", nullable = false, length = SOURCE_ID_LENGTH)
    private String sourceId;

    /**
     * Stores the source-local configuration key.
     */
    @Column(name = "config_key", nullable = false, length = CONFIG_KEY_LENGTH)
    private String key;

    /**
     * Creates an empty identity for JPA materialization.
     */
    protected JpaConfigEntryId() {
    }

    private JpaConfigEntryId(String sourceId, String key) {
        this.sourceId = sourceId;
        this.key = key;
    }

    /**
     * Creates one storage identity from a source and a configuration key.
     *
     * @param sourceId  the persistent source partition
     * @param configKey the source-local configuration key
     * @return the validated storage identity
     */
    static JpaConfigEntryId of(ConfigSourceId sourceId, ConfigKey configKey) {
        String sourceValue = Objects.requireNonNull(sourceId, "sourceId").getValue();
        String keyValue = Objects.requireNonNull(configKey, "configKey").getValue();
        requireLength(sourceValue, SOURCE_ID_LENGTH, "source ID");
        requireLength(keyValue, CONFIG_KEY_LENGTH, "configuration key");
        return new JpaConfigEntryId(
                sourceValue,
                keyValue
        );
    }

    String getSourceId() {
        return sourceId;
    }

    String getKey() {
        return key;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof JpaConfigEntryId identifier)) {
            return false;
        }
        return sourceId.equals(identifier.sourceId) && key.equals(identifier.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceId, key);
    }

    /**
     * Rejects values that cannot be represented by the canonical schema.
     */
    private static void requireLength(String value, int maximumLength, String label) {
        if (value.length() > maximumLength) {
            throw new IllegalArgumentException(label + " exceeds the JPA storage limit");
        }
    }
}
