package com.lamprism.luxspec.config.resolution;

import com.lamprism.luxspec.config.source.ConfigSourceId;

import java.util.Objects;

/**
 * Describes where a configuration result originated.
 *
 * @author RollW
 */
public sealed interface ConfigValueOrigin
        permits ConfigValueOrigin.SourceOrigin,
        ConfigValueOrigin.DefaultOrigin,
        ConfigValueOrigin.AbsentOrigin,
        ConfigValueOrigin.TombstoneOrigin {
    /**
     * Identifies a result observed from one source instance.
     *
     * @param sourceId the non-null source ID
     */
    record SourceOrigin(ConfigSourceId sourceId) implements ConfigValueOrigin {
        /**
         * Creates a source origin.
         *
         * @param sourceId the source ID
         */
        public SourceOrigin {
            Objects.requireNonNull(sourceId, "sourceId");
        }
    }

    /**
     * Identifies a value supplied by the Spec default.
     */
    enum DefaultOrigin implements ConfigValueOrigin {
        /**
         * The Spec default supplied the value.
         */
        INSTANCE
    }

    /**
     * Identifies an absence after all eligible observations were evaluated.
     */
    enum AbsentOrigin implements ConfigValueOrigin {
        /**
         * No source value or default was available.
         */
        INSTANCE
    }

    /**
     * Identifies a source tombstone that suppresses lower-priority fallback.
     *
     * @param sourceId the non-null tombstone source ID
     */
    record TombstoneOrigin(ConfigSourceId sourceId) implements ConfigValueOrigin {
        /**
         * Creates a tombstone origin.
         *
         * @param sourceId the source ID
         */
        public TombstoneOrigin {
            Objects.requireNonNull(sourceId, "sourceId");
        }
    }
}
