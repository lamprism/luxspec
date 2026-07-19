package com.lamprism.luxspec.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CachingConfigReaderTest {
    @Test
    void invalidationPolicyCachesOneKeyUntilExplicitInvalidation() {
        AtomicInteger reads = new AtomicInteger();
        ConfigReader delegate = new ConfigReader() {
            @Override
            public <T> ResolvedConfig<T> get(ConfigSpec<T> spec) {
                Integer value = reads.incrementAndGet();
                @SuppressWarnings("unchecked")
                ResolvedConfig<T> resolved = (ResolvedConfig<T>) ResolvedConfig.source(value, ConfigSourceId.of("counting"));
                return resolved;
            }
        };
        ConfigSpec<Integer> spec = FixedConfigSpec.of(ConfigKey.of("sample.limit"), ConfigCodecs.integer(), null, false);
        CachingConfigReader reader = new CachingConfigReader(delegate, key -> ConfigCachePolicy.invalidation());

        assertEquals(1, reader.get(spec).getValue().orElseThrow());
        assertEquals(1, reader.get(spec).getValue().orElseThrow());
        reader.invalidate(spec.getKey());
        assertEquals(2, reader.get(spec).getValue().orElseThrow());
    }

    @Test
    void freshReadBypassesTheCache() {
        AtomicInteger reads = new AtomicInteger();
        ConfigReader delegate = new ConfigReader() {
            @Override
            public <T> ResolvedConfig<T> get(ConfigSpec<T> spec) {
                Integer value = reads.incrementAndGet();
                @SuppressWarnings("unchecked")
                ResolvedConfig<T> resolved = (ResolvedConfig<T>) ResolvedConfig.source(value, ConfigSourceId.of("counting"));
                return resolved;
            }
        };
        ConfigSpec<Integer> spec = FixedConfigSpec.of(ConfigKey.of("sample.limit"), ConfigCodecs.integer(), null, false);
        CachingConfigReader reader = new CachingConfigReader(delegate, key -> ConfigCachePolicy.invalidation());

        assertEquals(1, reader.get(spec).getValue().orElseThrow());
        assertEquals(2, reader.get(spec, ConfigReadOption.FRESH).getValue().orElseThrow());
        assertEquals(1, reader.get(spec).getValue().orElseThrow());
    }
}
