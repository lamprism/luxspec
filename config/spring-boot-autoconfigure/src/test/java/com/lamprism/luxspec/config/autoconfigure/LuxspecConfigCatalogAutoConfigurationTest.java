/*
 * Copyright (C) Lamprism
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

/**
 *
 * @author RollW
 */
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
