package com.lamprism.luxspec.config.cache;

import com.lamprism.luxspec.config.ConfigBinding;
import com.lamprism.luxspec.config.ConfigValue;

/**
 * Internal cache bypass capability used by write consistency coordination.
 *
 * @author RollW
 */
public interface FreshConfigReader {
    /**
     * Resolves a binding without using a cached value.
     *
     * @param binding the concrete configuration binding
     * @param <T>     the decoded value type
     * @return the fresh configuration value
     */
    <T> ConfigValue<T> getFresh(ConfigBinding<T> binding);
}
