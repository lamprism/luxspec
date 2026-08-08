package com.lamprism.luxspec.config;

import com.lamprism.luxspec.cache.CacheProfile;
import com.lamprism.luxspec.cache.CaffeineCaches;
import com.lamprism.luxspec.config.cache.ConfigCacheKey;
import com.lamprism.luxspec.config.provider.CachingConfigProvider;
import com.lamprism.luxspec.config.runtime.SourceConfigWriter;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        CachingConfigProvider provider = new CachingConfigProvider(
                delegate,
                CaffeineCaches.create(CacheProfile.builder().maximumSize(16).build())
        );
        ConfigSpec<Integer> spec = ConfigSpec.of(
                "sample.limit",
                ConfigCodecs.integer(),
                null,
                false
        );

        assertEquals(1, provider.get(spec).getValue());
        assertEquals(1, provider.get(spec).getValue());
        provider.invalidate(ConfigCacheKey.of(spec.bind()));
        assertEquals(2, provider.get(spec).getValue());
    }

    @Test
    void freshReadBypassesTheInjectedCache() {
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
        CachingConfigProvider provider = new CachingConfigProvider(
                delegate,
                CaffeineCaches.create(CacheProfile.defaults())
        );
        ConfigSpec<Integer> spec = ConfigSpec.of(
                "sample.limit",
                ConfigCodecs.integer(),
                null,
                false
        );

        assertEquals(1, provider.get(spec).getValue());
        assertEquals(2, provider.getFresh(spec.bind()).getValue());
        assertEquals(1, provider.get(spec).getValue());
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
        CachingConfigProvider provider = new CachingConfigProvider(delegate, CaffeineCaches.create());
        ConfigSpec<Integer> spec = ConfigSpec.builder("security.limit", ConfigCodecs.integer())
                .sensitive()
                .build();

        assertEquals(1, provider.get(spec).getValue());
        assertEquals(2, provider.get(spec).getValue());
    }
}
