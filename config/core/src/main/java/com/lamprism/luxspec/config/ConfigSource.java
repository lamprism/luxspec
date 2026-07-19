package com.lamprism.luxspec.config;

import java.util.Map;
import java.util.Set;

/**
 * Reads and optionally mutates raw entries for one configuration source instance.
 *
 * @author RollW
 */
public interface ConfigSource {
    /**
     * Returns the stable source instance identifier.
     *
     * @return the source identifier
     */
    ConfigSourceId getId();

    /**
     * Returns the operations and storage properties supported by this source.
     *
     * @return immutable source capabilities
     */
    Set<ConfigSourceCapability> getCapabilities();

    /**
     * Returns provider-defined attributes used by source constraints.
     *
     * @return immutable source attributes
     */
    default Map<String, String> getAttributes() {
        return Map.of();
    }

    /**
     * Reads the raw entry for one complete key.
     *
     * @param key the complete configuration key
     * @return the explicit raw entry state
     */
    ConfigEntry get(ConfigKey key);

    /**
     * Stores one raw value for a complete key.
     *
     * @param key the complete configuration key
     * @param rawValue the provider-neutral value to store
     * @throws UnsupportedOperationException when this source is not writable
     */
    default void set(ConfigKey key, RawConfigValue rawValue) {
        throw new UnsupportedOperationException("Source is not writable");
    }

    /**
     * Removes one raw entry.
     *
     * @param key the complete configuration key
     * @throws UnsupportedOperationException when this source is not writable
     */
    default void remove(ConfigKey key) {
        throw new UnsupportedOperationException("Source is not writable");
    }

    /**
     * Stores an explicit lower-layer mask for one complete key.
     *
     * @param key the complete configuration key
     * @throws UnsupportedOperationException when this source cannot store masks
     */
    default void mask(ConfigKey key) {
        throw new UnsupportedOperationException("Source does not support masks");
    }
}
