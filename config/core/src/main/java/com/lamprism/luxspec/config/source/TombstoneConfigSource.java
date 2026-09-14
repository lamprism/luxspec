package com.lamprism.luxspec.config.source;

import com.lamprism.luxspec.config.ConfigKey;

/**
 * Adds optional tombstone support to a writable Source.
 *
 * <p>This role is intentionally outside the ordinary typed Writer and Provider contracts. A
 * tombstone suppresses lower layers without becoming a typed configuration value.</p>
 *
 * @author RollW
 */
public interface TombstoneConfigSource extends WritableConfigSource {
    /**
     * Stores a tombstone for one complete key.
     *
     * @param key the complete configuration key
     */
    void writeTombstone(ConfigKey key);
}
