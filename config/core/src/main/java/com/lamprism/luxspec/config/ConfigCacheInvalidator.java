package com.lamprism.luxspec.config;

/**
 * Invalidates cached effective configuration for one complete key.
 *
 * @author RollW
 */
@FunctionalInterface
public interface ConfigCacheInvalidator {
    /**
     * Invalidates cached resolution for one key.
     *
     * @param key the affected complete key
     */
    void invalidate(ConfigKey key);
}
