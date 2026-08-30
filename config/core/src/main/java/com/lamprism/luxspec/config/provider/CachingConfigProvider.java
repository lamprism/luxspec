package com.lamprism.luxspec.config.provider;

import com.lamprism.luxspec.config.ConfigBinding;
import com.lamprism.luxspec.config.ConfigProvider;
import com.lamprism.luxspec.config.ConfigValue;
import com.lamprism.luxspec.config.cache.ConfigCacheRule;
import com.lamprism.luxspec.config.cache.ConfigValueCache;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.ConfigSourceScope;

import java.util.Objects;

/**
 * Decorates a Provider with a configuration-owned value cache.
 *
 * <p>Cache retention is Provider assembly state. No cache settings are read from a ConfigSpec.</p>
 *
 * @author RollW
 */
public class CachingConfigProvider implements ConfigProvider {
    private final ConfigProvider delegate;
    private final ConfigValueCache cache;
    private final ConfigCacheRule cacheRule;

    /**
     * Creates a Provider that caches non-sensitive delegated results.
     *
     * @param delegate the underlying Provider
     * @param cache    the configuration-owned value cache
     */
    public CachingConfigProvider(
            ConfigProvider delegate,
            ConfigValueCache cache
    ) {
        this(delegate, cache, ConfigCacheRule.nonSensitiveOnly());
    }

    /**
     * Creates a Provider with an assembly-level cache rule.
     *
     * @param delegate  the underlying Provider
     * @param cache     the configuration-owned value cache
     * @param cacheRule the cache retention rule
     */
    public CachingConfigProvider(
            ConfigProvider delegate,
            ConfigValueCache cache,
            ConfigCacheRule cacheRule
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.cache = Objects.requireNonNull(cache, "cache");
        this.cacheRule = Objects.requireNonNull(cacheRule, "cacheRule");
    }

    @Override
    public ConfigSourceScope getSourceScope() {
        return delegate.getSourceScope();
    }

    @Override
    public <T> ConfigValue<T> get(ConfigBinding<T> binding) {
        ConfigBinding<T> nonNullBinding = Objects.requireNonNull(binding, "binding");
        if (!cacheRule.shouldCache(nonNullBinding)) {
            return delegate.get(nonNullBinding);
        }
        return cache.get(nonNullBinding, () -> delegate.get(nonNullBinding));
    }

    @Override
    public <T> void set(ConfigBinding<T> binding, T value) {
        ConfigBinding<T> nonNullBinding = Objects.requireNonNull(binding, "binding");
        mutate(nonNullBinding, () -> delegate.set(nonNullBinding, value));
    }

    @Override
    public <T> void set(ConfigSourceId sourceId, ConfigBinding<T> binding, T value) {
        ConfigBinding<T> nonNullBinding = Objects.requireNonNull(binding, "binding");
        mutate(nonNullBinding, () -> delegate.set(sourceId, nonNullBinding, value));
    }

    @Override
    public void remove(ConfigBinding<?> binding) {
        ConfigBinding<?> nonNullBinding = Objects.requireNonNull(binding, "binding");
        mutate(nonNullBinding, () -> delegate.remove(nonNullBinding));
    }

    @Override
    public void remove(ConfigSourceId sourceId, ConfigBinding<?> binding) {
        ConfigBinding<?> nonNullBinding = Objects.requireNonNull(binding, "binding");
        mutate(nonNullBinding, () -> delegate.remove(sourceId, nonNullBinding));
    }

    private void mutate(ConfigBinding<?> binding, Runnable mutation) {
        ConfigBinding<?> nonNullBinding = Objects.requireNonNull(binding, "binding");
        Runnable nonNullMutation = Objects.requireNonNull(mutation, "mutation");
        cache.invalidate(nonNullBinding.getKey());
        RuntimeException mutationFailure = null;
        try {
            nonNullMutation.run();
        } catch (RuntimeException failure) {
            mutationFailure = failure;
            throw failure;
        } finally {
            try {
                cache.invalidate(nonNullBinding.getKey());
            } catch (RuntimeException invalidationFailure) {
                if (mutationFailure == null) {
                    throw invalidationFailure;
                }
                mutationFailure.addSuppressed(invalidationFailure);
            }
        }
    }
}
