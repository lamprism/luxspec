package com.lamprism.luxspec.config.autoconfigure;

import com.lamprism.luxspec.cache.Cache;
import com.lamprism.luxspec.cache.CacheFactory;
import com.lamprism.luxspec.cache.CacheName;
import com.lamprism.luxspec.cache.CacheProfile;
import com.lamprism.luxspec.config.ConfigBinding;
import com.lamprism.luxspec.config.ConfigCodecs;
import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigProvider;
import com.lamprism.luxspec.config.ConfigReader;
import com.lamprism.luxspec.config.ConfigSpec;
import com.lamprism.luxspec.config.ConfigValue;
import com.lamprism.luxspec.config.ConfigWriter;
import com.lamprism.luxspec.config.cache.ConfigValueCache;
import com.lamprism.luxspec.config.provider.CachingConfigProvider;
import com.lamprism.luxspec.config.resolution.ConfigValueOrigin;
import com.lamprism.luxspec.config.runtime.LayeredConfigReader;
import com.lamprism.luxspec.config.runtime.SourceConfigWriter;
import com.lamprism.luxspec.config.source.ConfigEntry;
import com.lamprism.luxspec.config.source.ConfigSource;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.ConfigSourceScope;
import com.lamprism.luxspec.config.source.RawConfigValue;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class LuxspecConfigAutoConfigurationTest {
    private static final ConfigSourceId SOURCE_ID = ConfigSourceId.of("test-source");
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LuxspecConfigAutoConfiguration.class));

    @Test
    void backsOffWithoutAnApplicationSource() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(ConfigReader.class);
            assertThat(context).doesNotHaveBean(ConfigWriter.class);
        });
    }

    @Test
    void createsReaderAndWriterFromApplicationSources() {
        contextRunner
                .withBean(ConfigSource.class, LuxspecConfigAutoConfigurationTest::testSource)
                .run(context -> {
                    assertThat(context).hasSingleBean(LayeredConfigReader.class);
                    assertThat(context).hasSingleBean(SourceConfigWriter.class);
                    assertThat(context).hasSingleBean(ConfigProvider.class);

                    ConfigSpec<Integer> spec = ConfigSpec.of(
                            "server.port",
                            ConfigCodecs.integer(),
                            null,
                            false
                    );
                    ConfigValue<Integer> resolved = context.getBean(ConfigProvider.class).get(spec);
                    assertThat(resolved.getValue()).isEqualTo(8080);
                    assertThat(resolved.getOrigin())
                            .isEqualTo(new ConfigValueOrigin.SourceOrigin(SOURCE_ID));
                });
    }

    @Test
    void createsAConfigurationValueCacheThroughAnApplicationFactory() {
        TestCacheFactory factory = new TestCacheFactory();
        CacheProfile profile = CacheProfile.builder().maximumSize(16).build();
        contextRunner
                .withBean(ConfigSource.class, LuxspecConfigAutoConfigurationTest::testSource)
                .withBean(CacheFactory.class, () -> factory)
                .withBean(CacheProfile.class, () -> profile)
                .run(context -> {
                    assertThat(context).hasSingleBean(ConfigValueCache.class);
                    assertThat(context.getBean(ConfigProvider.class)).isInstanceOf(CachingConfigProvider.class);
                    assertThat(factory.names).containsExactly(CacheName.of("config.values"));
                    assertThat(factory.profiles).containsExactly(profile);
                });
    }

    @Test
    void backsOffReaderWhenApplicationProvidesOne() {
        ConfigReader applicationReader = new StubConfigReader();

        contextRunner
                .withBean(ConfigSource.class, LuxspecConfigAutoConfigurationTest::testSource)
                .withBean(ConfigReader.class, () -> applicationReader)
                .run(context -> {
                    assertThat(context.getBeansOfType(ConfigReader.class).values())
                            .contains(applicationReader);
                    assertThat(context.getBeansOfType(LayeredConfigReader.class)).isEmpty();
                    assertThat(context.getBeansOfType(SourceConfigWriter.class))
                            .hasSize(1);
                });
    }

    @Test
    void backsOffWriterWhenApplicationProvidesOne() {
        ConfigWriter applicationWriter = new SourceConfigWriter(List.of(testSource()));

        contextRunner
                .withBean(ConfigSource.class, LuxspecConfigAutoConfigurationTest::testSource)
                .withBean(ConfigWriter.class, () -> applicationWriter)
                .run(context -> {
                    assertThat(context.getBeansOfType(ConfigWriter.class).values())
                            .contains(applicationWriter);
                    assertThat(context.getBeansOfType(ConfigReader.class).values())
                            .isNotEmpty();
                    assertThat(context.getBeansOfType(SourceConfigWriter.class))
                            .hasSize(1);
                });
    }

    @Test
    void assemblesProviderFromApplicationReaderAndDefaultWriter() {
        ConfigReader applicationReader = new StubConfigReader();

        contextRunner
                .withBean(ConfigSource.class, LuxspecConfigAutoConfigurationTest::testSource)
                .withBean(ConfigReader.class, () -> applicationReader)
                .run(context -> {
                    assertThat(context).hasSingleBean(ConfigProvider.class);
                    assertThat(context.getBean(ConfigProvider.class)).isNotNull();
                });
    }

    private static TestConfigSource testSource() {
        return new TestConfigSource(Map.of(
                ConfigKey.of("server.port"),
                ConfigEntry.present(RawConfigValue.integer(8080))
        ));
    }

    private static final class StubConfigReader implements ConfigReader {
        @Override
        public <T> ConfigValue<T> get(@NonNull ConfigBinding<T> binding) {
            return ConfigValue.absent();
        }
    }

    private static final class TestConfigSource implements ConfigSource {
        private final Map<ConfigKey, ConfigEntry> entries;

        private TestConfigSource(Map<ConfigKey, ConfigEntry> entries) {
            this.entries = Map.copyOf(entries);
        }

        @Override
        public ConfigSourceId getId() {
            return SOURCE_ID;
        }

        @Override
        public ConfigSourceScope getScope() {
            return ConfigSourceScope.RUNTIME;
        }

        @Override
        public ConfigEntry get(@NonNull ConfigKey key) {
            return entries.getOrDefault(key, ConfigEntry.absent());
        }
    }

    private static final class TestCache<K, V> implements Cache<K, V> {
        private final Map<K, V> values = new ConcurrentHashMap<>();

        @Override
        public V getIfPresent(K key) {
            return values.get(key);
        }

        @Override
        public V get(K key, Function<? super K, ? extends V> loader) {
            return values.computeIfAbsent(key, loader);
        }

        @Override
        public void put(K key, V value) {
            values.put(key, value);
        }

        @Override
        public void invalidate(K key) {
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
    }

    private static final class TestCacheFactory implements CacheFactory {
        private final List<CacheName> names = new ArrayList<>();
        private final List<CacheProfile> profiles = new ArrayList<>();

        @Override
        public <K, V> Cache<K, V> create(CacheName name, CacheProfile profile) {
            names.add(name);
            profiles.add(profile);
            return new TestCache<>();
        }
    }
}
