package com.lamprism.luxspec.config.source;

import com.lamprism.luxspec.config.ConfigKey;

/**
 * Adds ordinary mutation operations to the read-only source role.
 *
 * @author RollW
 */
public interface WritableConfigSource extends ConfigSource {
    /**
     * Stores one raw value for a complete key.
     *
     * @param key      the complete key
     * @param rawValue the provider-neutral raw value
     */
    void set(ConfigKey key, RawConfigValue rawValue);

    /**
     * Removes one explicit entry so lower layers may become effective.
     *
     * @param key the complete key
     */
    void remove(ConfigKey key);
}
