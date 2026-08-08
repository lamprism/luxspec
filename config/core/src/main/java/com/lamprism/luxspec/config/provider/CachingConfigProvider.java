package com.lamprism.luxspec.config.provider;

import com.lamprism.luxspec.cache.Cache;
import com.lamprism.luxspec.cache.CacheInvalidator;
import com.lamprism.luxspec.config.ConfigBinding;
import com.lamprism.luxspec.config.ConfigProvider;
import com.lamprism.luxspec.config.ConfigValue;
import com.lamprism.luxspec.config.cache.ConfigCacheKey;
import com.lamprism.luxspec.config.cache.ConfigCacheRule;
import com.lamprism.luxspec.config.cache.FreshConfigReader;
import com.lamprism.luxspec.config.source.ConfigSourceId;

import java.util.Objects;

/**
 * Decorates a Provider with an injected generic cache.
 *
 * <p>Cache retention is Provider assembly state. No cache settings are read from a ConfigSpec.</p>
 *
 * @author RollW
 */
public final class CachingConfigProvider implements ConfigProvider, FreshConfigReader,
        CacheInvalidator<ConfigCacheKey> {
    private final ConfigProvider delegate;
    private final Cache<ConfigCacheKey, ConfigValue<?>> cache;
    private final ConfigCacheRule cacheRule;

    /**
     * Creates a Provider that caches non-sensitive delegated results.
     *
     * @param delegate the underlying Provider
     * @param cache    the injected cache
     */
    public CachingConfigProvider(
            ConfigProvider delegate,
            Cache<ConfigCacheKey, ConfigValue<?>> cache
    ) {
        this(delegate, cache, ConfigCacheRule.nonSensitiveOnly());
    }

    /**
     * Creates a Provider with an assembly-level cache rule.
     *
     * @param delegate  the underlying Provider
     * @param cache     the injected cache
     * @param cacheRule the cache retention rule
     */
    public CachingConfigProvider(
            ConfigProvider delegate,
            Cache<ConfigCacheKey, ConfigValue<?>> cache,
            ConfigCacheRule cacheRule
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.cache = Objects.requireNonNull(cache, "cache");
        this.cacheRule = Objects.requireNonNull(cacheRule, "cacheRule");
    }

    @Override
    public <T> ConfigValue<T> get(ConfigBinding<T> binding) {
        ConfigBinding<T> nonNullBinding = Objects.requireNonNull(binding, "binding");
        if (!cacheRule.shouldCache(nonNullBinding)) {
            return delegate.get(nonNullBinding);
        }
        ConfigCacheKey cacheKey = ConfigCacheKey.of(nonNullBinding);
        ConfigValue<?> value = cache.get(cacheKey, ignored -> delegate.get(nonNullBinding));
        return cast(value);
    }

    @Override
    public <T> ConfigValue<T> getFresh(ConfigBinding<T> binding) {
        return delegate.get(Objects.requireNonNull(binding, "binding"));
    }

    @Override
    public <T> void set(ConfigBinding<T> binding, T value) {
        ConfigBinding<T> nonNullBinding = Objects.requireNonNull(binding, "binding");
        delegate.set(nonNullBinding, value);
        invalidate(nonNullBinding);
    }

    @Override
    public <T> void set(ConfigSourceId sourceId, ConfigBinding<T> binding, T value) {
        ConfigBinding<T> nonNullBinding = Objects.requireNonNull(binding, "binding");
        delegate.set(sourceId, nonNullBinding, value);
        invalidate(nonNullBinding);
    }

    @Override
    public void remove(ConfigBinding<?> binding) {
        ConfigBinding<?> nonNullBinding = Objects.requireNonNull(binding, "binding");
        delegate.remove(nonNullBinding);
        invalidate(nonNullBinding);
    }

    @Override
    public void remove(ConfigSourceId sourceId, ConfigBinding<?> binding) {
        ConfigBinding<?> nonNullBinding = Objects.requireNonNull(binding, "binding");
        delegate.remove(sourceId, nonNullBinding);
        invalidate(nonNullBinding);
    }

    @Override
    public void invalidate(ConfigCacheKey key) {
        cache.invalidate(Objects.requireNonNull(key, "key"));
    }

    private void invalidate(ConfigBinding<?> binding) {
        invalidate(ConfigCacheKey.of(binding));
    }

    @SuppressWarnings("unchecked")
    private static <T> ConfigValue<T> cast(ConfigValue<?> value) {
        return (ConfigValue<T>) value;
    }
}
