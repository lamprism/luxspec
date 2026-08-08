package com.lamprism.luxspec.config.event;

import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.event.Event;

/**
 * Common value-free contract for configuration change events.
 *
 * @author RollW
 */
public interface ConfigEvent extends Event {
    /**
     * Identifies the level at which a configuration event was produced.
     */
    enum Kind {
        /**
         * A raw Source mutation completed.
         */
        SOURCE_CHANGED,
        /**
         * The effective typed result changed.
         */
        EFFECTIVE_CHANGED
    }

    /**
     * Returns the affected complete key.
     *
     * @return the configuration key
     */
    ConfigKey getKey();

    /**
     * Returns the event kind.
     *
     * @return the event kind
     */
    Kind getKind();
}
