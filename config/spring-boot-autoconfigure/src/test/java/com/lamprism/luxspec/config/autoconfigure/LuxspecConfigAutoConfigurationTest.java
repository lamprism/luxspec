package com.lamprism.luxspec.config.autoconfigure;

import com.lamprism.luxspec.config.ConfigBinding;
import com.lamprism.luxspec.config.ConfigCodecs;
import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigProvider;
import com.lamprism.luxspec.config.ConfigReader;
import com.lamprism.luxspec.config.ConfigSpec;
import com.lamprism.luxspec.config.ConfigValue;
import com.lamprism.luxspec.config.ConfigWriter;
import com.lamprism.luxspec.config.resolution.ConfigValueOrigin;
import com.lamprism.luxspec.config.runtime.LayeredConfigReader;
import com.lamprism.luxspec.config.runtime.SourceConfigWriter;
import com.lamprism.luxspec.config.source.ConfigEntry;
import com.lamprism.luxspec.config.source.ConfigSource;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.RawConfigValue;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;
import java.util.Map;

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
    void backsOffReaderWhenApplicationProvidesOne() {
        ConfigReader applicationReader = new StubConfigReader();

        contextRunner
                .withBean(ConfigSource.class, LuxspecConfigAutoConfigurationTest::testSource)
                .withBean(ConfigReader.class, () -> applicationReader)
                .run(context -> {
                    assertThat(context.getBean(ConfigReader.class)).isSameAs(applicationReader);
                    assertThat(context.getBeansOfType(LayeredConfigReader.class)).isEmpty();
                    assertThat(context).hasSingleBean(ConfigWriter.class);
                });
    }

    @Test
    void backsOffWriterWhenApplicationProvidesOne() {
        ConfigWriter applicationWriter = new SourceConfigWriter(List.of(testSource()));

        contextRunner
                .withBean(ConfigSource.class, LuxspecConfigAutoConfigurationTest::testSource)
                .withBean(ConfigWriter.class, () -> applicationWriter)
                .run(context -> {
                    assertThat(context.getBean(ConfigWriter.class)).isSameAs(applicationWriter);
                    assertThat(context).hasSingleBean(ConfigReader.class);
                    assertThat(context.getBeansOfType(SourceConfigWriter.class))
                            .hasSize(1);
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
        public ConfigEntry get(ConfigKey key) {
            return entries.getOrDefault(key, ConfigEntry.absent());
        }
    }
}
