package com.lamprism.luxspec.config.cache;

import com.lamprism.luxspec.config.ConfigBinding;
import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigSpec;

import java.util.Objects;

/**
 * Identifies one typed configuration cache entry by Spec identity and complete key.
 *
 * @author RollW
 */
public final class ConfigCacheKey {
    private final ConfigSpec<?> spec;
    private final ConfigKey key;

    private ConfigCacheKey(ConfigSpec<?> spec, ConfigKey key) {
        this.spec = Objects.requireNonNull(spec, "spec");
        this.key = Objects.requireNonNull(key, "key");
    }

    /**
     * Creates a cache key from one binding.
     *
     * @param binding the complete typed binding
     * @return the cache key
     */
    public static ConfigCacheKey of(ConfigBinding<?> binding) {
        ConfigBinding<?> nonNullBinding = Objects.requireNonNull(binding, "binding");
        return new ConfigCacheKey(nonNullBinding.getSpec(), nonNullBinding.getKey());
    }

    /**
     * Returns the owning definition identity.
     *
     * @return the owning Spec
     */
    public ConfigSpec<?> getSpec() {
        return spec;
    }

    /**
     * Returns the complete key.
     *
     * @return the complete key
     */
    public ConfigKey getKey() {
        return key;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ConfigCacheKey cacheKey)) {
            return false;
        }
        return spec == cacheKey.spec && key.equals(cacheKey.key);
    }

    @Override
    public int hashCode() {
        return 31 * System.identityHashCode(spec) + key.hashCode();
    }
}
