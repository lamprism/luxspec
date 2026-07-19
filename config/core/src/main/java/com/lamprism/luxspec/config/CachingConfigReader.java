package com.lamprism.luxspec.config;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Decorates a reader with per-key no-cache, invalidation, or TTL policies.
 *
 * @author RollW
 */
public final class CachingConfigReader implements ConfigReader, ConfigCacheInvalidator {
    private final ConfigReader delegate;
    private final ConfigCachePolicyResolver policyResolver;
    private final Clock clock;
    private final ConcurrentMap<ConfigKey, CachedValue> values = new ConcurrentHashMap<>();

    /**
     * Creates a caching reader with the system UTC clock.
     *
     * @param delegate the uncached reader
     * @param policyResolver the external cache policy resolver
     */
    public CachingConfigReader(ConfigReader delegate, ConfigCachePolicyResolver policyResolver) {
        this(delegate, policyResolver, Clock.systemUTC());
    }

    /**
     * Creates a caching reader with an explicit clock.
     *
     * @param delegate the uncached reader
     * @param policyResolver the external cache policy resolver
     * @param clock the cache-expiration clock
     */
    public CachingConfigReader(ConfigReader delegate, ConfigCachePolicyResolver policyResolver, Clock clock) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.policyResolver = Objects.requireNonNull(policyResolver, "policyResolver");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public <T> ResolvedConfig<T> get(ConfigSpec<T> spec) {
        return get(spec, ConfigReadOption.CACHED);
    }

    @Override
    public <T> ResolvedConfig<T> get(ConfigSpec<T> spec, ConfigReadOption option) {
        ConfigSpec<T> nonNullSpec = Objects.requireNonNull(spec, "spec");
        if (Objects.requireNonNull(option, "option") == ConfigReadOption.FRESH) {
            return delegate.get(nonNullSpec, ConfigReadOption.FRESH);
        }
        ConfigCachePolicy policy = Objects.requireNonNull(policyResolver.getPolicy(nonNullSpec.getKey()), "cache policy");
        if (policy.getMode() == ConfigCachePolicy.Mode.NONE) {
            return delegate.get(nonNullSpec, ConfigReadOption.FRESH);
        }
        CachedValue cached = values.compute(nonNullSpec.getKey(), (key, existing) -> load(nonNullSpec, policy, existing));
        return cast(cached.resolved);
    }

    @Override
    public void invalidate(ConfigKey key) {
        values.remove(Objects.requireNonNull(key, "key"));
    }

    private <T> CachedValue load(ConfigSpec<T> spec, ConfigCachePolicy policy, CachedValue existing) {
        Instant now = clock.instant();
        if (existing != null && !existing.isExpired(now)) {
            return existing;
        }
        ResolvedConfig<T> resolved = delegate.get(spec, ConfigReadOption.FRESH);
        Instant expiresAt = expiration(policy, now);
        return new CachedValue(resolved, expiresAt);
    }

    private Instant expiration(ConfigCachePolicy policy, Instant now) {
        if (policy.getMode() == ConfigCachePolicy.Mode.INVALIDATION) {
            return Instant.MAX;
        }
        return now.plus(policy.requireTimeToLive());
    }

    @SuppressWarnings("unchecked")
    private <T> ResolvedConfig<T> cast(ResolvedConfig<?> resolved) {
        return (ResolvedConfig<T>) resolved;
    }

    private static final class CachedValue {
        private final ResolvedConfig<?> resolved;
        private final Instant expiresAt;

        private CachedValue(ResolvedConfig<?> resolved, Instant expiresAt) {
            this.resolved = Objects.requireNonNull(resolved, "resolved");
            this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        }

        private boolean isExpired(Instant now) {
            return !expiresAt.isAfter(now);
        }
    }
}
