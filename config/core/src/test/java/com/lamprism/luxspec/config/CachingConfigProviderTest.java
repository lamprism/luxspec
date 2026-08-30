package com.lamprism.luxspec.config;

import com.lamprism.luxspec.cache.Cache;
import com.lamprism.luxspec.cache.CacheFactory;
import com.lamprism.luxspec.cache.CacheName;
import com.lamprism.luxspec.cache.CachePlan;
import com.lamprism.luxspec.cache.CacheProfile;
import com.lamprism.luxspec.cache.CaffeineCacheFactory;
import com.lamprism.luxspec.config.cache.ConfigCacheKey;
import com.lamprism.luxspec.config.cache.ConfigValueCache;
import com.lamprism.luxspec.config.provider.CachingConfigProvider;
import com.lamprism.luxspec.config.runtime.SourceConfigWriter;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.ConfigSourceScope;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CachingConfigProviderTest {
    @Test
    void invalidationCachesOneBindingUntilExplicitInvalidation() {
        AtomicInteger reads = new AtomicInteger();
        ConfigReader delegateReader = new ConfigReader() {
            @Override
            public <T> ConfigValue<T> get(@NonNull ConfigBinding<T> binding) {
                Integer value = reads.incrementAndGet();
                @SuppressWarnings("unchecked")
                ConfigValue<T> result = (ConfigValue<T>) ConfigValue.source(
                        value,
                        ConfigSourceId.of("counting")
                );
                return result;
            }
        };
        ConfigProvider delegate = ConfigProvider.of(delegateReader, new SourceConfigWriter(List.of()));
        CacheProfile profile = CacheProfile.builder().maximumSize(16).build();
        ConfigValueCache cache = new ConfigValueCache(CachePlan.single(
                new CaffeineCacheFactory(),
                profile
        ));
        CachingConfigProvider provider = new CachingConfigProvider(delegate, cache);
        ConfigSpec<Integer> spec = ConfigSpec.of(
                "sample.limit",
                ConfigCodecs.integer(),
                null,
                false
        );

        assertEquals(1, provider.get(spec).getValue());
        assertEquals(1, provider.get(spec).getValue());
        cache.invalidate(spec.bind().getKey());
        assertEquals(2, provider.get(spec).getValue());
    }

    @Test
    void storesDefinitionsIndividuallyAndInvalidatesByCompleteKey() {
        RecordingCacheFactory factory = new RecordingCacheFactory();
        ConfigValueCache cache = new ConfigValueCache(CachePlan.single(
                factory,
                CacheProfile.defaults()
        ));
        ConfigSpec<Integer> firstSpec = ConfigSpec.of(
                "sample.limit",
                ConfigCodecs.integer(),
                null,
                false
        );
        ConfigSpec<Integer> secondSpec = ConfigSpec.of(
                "sample.limit",
                ConfigCodecs.integer(),
                null,
                false
        );

        cache.get(firstSpec.bind(), () -> ConfigValue.source(1, ConfigSourceId.of("counting")));
        cache.get(secondSpec.bind(), () -> ConfigValue.source(2, ConfigSourceId.of("counting")));

        assertEquals(2, factory.cacheSize());
        assertTrue(factory.cachedKeys().stream().allMatch(ConfigCacheKey.class::isInstance));

        cache.invalidate(firstSpec.bind().getKey());

        assertEquals(0, factory.cacheSize());
    }

    @Test
    void doesNotCacheSensitiveDefinitionsByDefault() {
        AtomicInteger reads = new AtomicInteger();
        ConfigReader delegateReader = new ConfigReader() {
            @Override
            public <T> ConfigValue<T> get(@NonNull ConfigBinding<T> binding) {
                int value = reads.incrementAndGet();
                @SuppressWarnings("unchecked")
                ConfigValue<T> result = (ConfigValue<T>) ConfigValue.source(
                        value,
                        ConfigSourceId.of("counting")
                );
                return result;
            }
        };
        ConfigProvider delegate = ConfigProvider.of(delegateReader, new SourceConfigWriter(List.of()));
        CachingConfigProvider provider = new CachingConfigProvider(
                delegate,
                new ConfigValueCache(CachePlan.single(
                        new CaffeineCacheFactory(),
                        CacheProfile.defaults()
                ))
        );
        ConfigSpec<Integer> spec = ConfigSpec.builder("security.limit", ConfigCodecs.integer())
                .sensitive()
                .build();

        assertEquals(1, provider.get(spec).getValue());
        assertEquals(2, provider.get(spec).getValue());
    }

    @Test
    void invalidatesAValueRepopulatedWhileAMutationIsInProgress() {
        AtomicInteger current = new AtomicInteger(1);
        AtomicReference<CachingConfigProvider> providerReference = new AtomicReference<>();
        ConfigProvider delegate = new ConfigProvider() {
            @Override
            public ConfigSourceScope getSourceScope() {
                return ConfigSourceScope.RUNTIME;
            }

            @Override
            public <T> ConfigValue<T> get(@NonNull ConfigBinding<T> binding) {
                @SuppressWarnings("unchecked")
                ConfigValue<T> value = (ConfigValue<T>) ConfigValue.source(
                        current.get(),
                        ConfigSourceId.of("mutable")
                );
                return value;
            }

            @Override
            public <T> void set(@NonNull ConfigBinding<T> binding, @NonNull T value) {
                providerReference.get().get(binding);
                current.set((Integer) value);
            }

            @Override
            public <T> void set(
                    @NonNull ConfigSourceId sourceId,
                    @NonNull ConfigBinding<T> binding,
                    @NonNull T value
            ) {
                set(binding, value);
            }

            @Override
            public void remove(@NonNull ConfigBinding<?> binding) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void remove(@NonNull ConfigSourceId sourceId, @NonNull ConfigBinding<?> binding) {
                remove(binding);
            }
        };
        CachingConfigProvider provider = new CachingConfigProvider(
                delegate,
                new ConfigValueCache(CachePlan.single(
                        new CaffeineCacheFactory(),
                        CacheProfile.defaults()
                ))
        );
        providerReference.set(provider);
        ConfigSpec<Integer> spec = ConfigSpec.of("sample.limit", ConfigCodecs.integer(), null, false);

        assertEquals(1, provider.get(spec).getValue());
        provider.set(spec, 2);

        assertEquals(2, provider.get(spec).getValue());
    }

    private static final class RecordingCacheFactory implements CacheFactory {
        private RecordingCache<?, ?> cache;

        @Override
        public <K, V> Cache<K, V> create(
                @NonNull CacheName name,
                @NonNull CacheProfile profile
        ) {
            RecordingCache<K, V> created = new RecordingCache<>();
            cache = created;
            return created;
        }

        private int cacheSize() {
            return cache.size();
        }

        private Set<?> cachedKeys() {
            return cache.keys();
        }
    }

    private static final class RecordingCache<K, V> implements Cache<K, V> {
        private final Map<K, V> values = new ConcurrentHashMap<>();

        @Override
        public V getIfPresent(K key) {
            return values.get(key);
        }

        @Override
        public V get(@NonNull K key, @NonNull Function<? super K, ? extends V> loader) {
            return values.computeIfAbsent(key, loader);
        }

        @Override
        public void put(@NonNull K key, @NonNull V value) {
            values.put(key, value);
        }

        @Override
        public void invalidate(@NonNull K key) {
            values.remove(key);
        }

        @Override
        public void invalidateAll() {
            values.clear();
        }

        @Override
        public void invalidateAll(Predicate<? super K> predicate) {
            values.keySet().removeIf(predicate);
        }

        private int size() {
            return values.size();
        }

        private Set<K> keys() {
            return Set.copyOf(values.keySet());
        }
    }
}
