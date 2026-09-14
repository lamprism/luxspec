package com.lamprism.luxspec.config.source;

import com.lamprism.luxspec.config.ConfigKey;

import java.util.Map;

/**
 * Reads raw entries for one configuration source instance.
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
     * Returns the lifecycle scope in which this source is available.
     *
     * @return the source scope
     */
    ConfigSourceScope getScope();

    /**
     * Returns provider-defined attributes used by source-selection policies.
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

}
