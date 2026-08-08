package com.lamprism.luxspec.config.cache;

import com.lamprism.luxspec.config.ConfigBinding;

/**
 * Selects which bindings a caching Provider may retain.
 *
 * <p>This is Provider assembly configuration, not ConfigSpec metadata.</p>
 *
 * @author RollW
 */
@FunctionalInterface
public interface ConfigCacheRule {
    /**
     * Reports whether a binding may be cached.
     *
     * @param binding the concrete binding
     * @return {@code true} when the Provider may cache its result
     */
    boolean shouldCache(ConfigBinding<?> binding);

    /**
     * Returns a rule that permits every binding.
     *
     * @return the allow-all rule
     */
    static ConfigCacheRule always() {
        return binding -> true;
    }

    /**
     * Returns a rule that disables caching for every binding.
     *
     * @return the deny-all rule
     */
    static ConfigCacheRule never() {
        return binding -> false;
    }

    /**
     * Returns the conservative default that excludes sensitive definitions.
     *
     * @return the non-sensitive-only rule
     */
    static ConfigCacheRule nonSensitiveOnly() {
        return binding -> !binding.getSpec().isSensitive();
    }
}
