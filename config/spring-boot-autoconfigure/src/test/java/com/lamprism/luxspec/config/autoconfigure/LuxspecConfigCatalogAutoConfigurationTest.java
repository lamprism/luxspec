package com.lamprism.luxspec.config.autoconfigure;

import com.lamprism.luxspec.config.ConfigCodec;
import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigSpec;
import com.lamprism.luxspec.config.catalog.ConfigCatalog;
import com.lamprism.luxspec.config.catalog.ConfigSpecContributor;
import com.lamprism.luxspec.config.catalog.InMemoryConfigCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class LuxspecConfigCatalogAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LuxspecConfigCatalogAutoConfiguration.class));

    @Test
    void createsCatalogFromApplicationContributors() {
        ConfigSpec<String> spec = ConfigSpec.of(
                "feature.name",
                ConfigCodec.string(),
                null,
                false
        );

        contextRunner
                .withBean(ConfigSpecContributor.class, () -> registry -> registry.register(spec))
                .run(context -> {
                    assertThat(context).hasSingleBean(ConfigCatalog.class);
                    ConfigCatalog catalog = context.getBean(ConfigCatalog.class);
                    assertThat(catalog.getDefinitions()).containsExactly(spec);
                    assertThat(catalog.resolve(ConfigKey.of("feature.name")).getSpec())
                            .isSameAs(spec);
                });
    }

    @Test
    void backsOffWithoutContributors() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(ConfigCatalog.class));
    }

    @Test
    void backsOffWhenApplicationProvidesCatalog() {
        ConfigCatalog applicationCatalog = new InMemoryConfigCatalog();

        contextRunner
                .withBean(ConfigSpecContributor.class, () -> registry -> {
                })
                .withBean(ConfigCatalog.class, () -> applicationCatalog)
                .run(context -> assertThat(context.getBean(ConfigCatalog.class))
                        .isSameAs(applicationCatalog));
    }
}
