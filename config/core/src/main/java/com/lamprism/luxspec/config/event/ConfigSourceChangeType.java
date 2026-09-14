package com.lamprism.luxspec.config.event;

/**
 * Identifies the raw mutation completed by a configuration source.
 *
 * @author RollW
 */
public enum ConfigSourceChangeType {
    /**
     * A value was written.
     */
    SET,
    /**
     * A value was removed.
     */
    REMOVE
}
