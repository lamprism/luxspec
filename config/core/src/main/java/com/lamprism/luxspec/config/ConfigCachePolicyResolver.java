package com.lamprism.luxspec.config;

/**
 * Selects cache policy outside configuration definition metadata.
 *
 * @author RollW
 */
@FunctionalInterface
public interface ConfigCachePolicyResolver {
    /**
     * Returns the cache policy for one complete key.
     *
     * @param key the complete configuration key
     * @return the cache policy
     */
    ConfigCachePolicy getPolicy(ConfigKey key);
}
