package com.lamprism.luxspec.config.policy;

import com.lamprism.luxspec.config.source.ConfigSourceId;

import java.util.Objects;

/**
 * Selects which configured Source instances a source-selection policy may use.
 *
 * @author RollW
 */
public interface ConfigSourceSelector {
    /**
     * Creates a selector that accepts every Source ID.
     *
     * @return the any-Source selector
     */
    static ConfigSourceSelector any() {
        return AnySource.INSTANCE;
    }

    /**
     * Creates a selector for exactly one Source instance.
     *
     * @param sourceId the required Source ID
     * @return the exact-Source selector
     */
    static ConfigSourceSelector exact(ConfigSourceId sourceId) {
        return new ExactSource(Objects.requireNonNull(sourceId, "sourceId"));
    }

    /**
     * Reports whether a Source ID satisfies this selector.
     *
     * @param sourceId the candidate Source ID
     * @return {@code true} when the Source is selected
     */
    boolean matches(ConfigSourceId sourceId);

    /**
     * Selects every non-null Source ID.
     */
    final class AnySource implements ConfigSourceSelector {
        private static final AnySource INSTANCE = new AnySource();

        private AnySource() {
        }

        @Override
        public boolean matches(ConfigSourceId sourceId) {
            Objects.requireNonNull(sourceId, "sourceId");
            return true;
        }
    }

    /**
     * Selects one exact Source ID.
     *
     * @param sourceId the selected Source ID
     */
    record ExactSource(ConfigSourceId sourceId) implements ConfigSourceSelector {
        /**
         * Creates an exact Source selector.
         *
         * @param sourceId the selected Source ID
         */
        public ExactSource {
            Objects.requireNonNull(sourceId, "sourceId");
        }

        @Override
        public boolean matches(ConfigSourceId candidate) {
            return sourceId.equals(Objects.requireNonNull(candidate, "candidate"));
        }
    }
}
