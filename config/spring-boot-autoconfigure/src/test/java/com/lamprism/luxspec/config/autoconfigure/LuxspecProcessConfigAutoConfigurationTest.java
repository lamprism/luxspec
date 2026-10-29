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

import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.source.ConfigEntry;
import com.lamprism.luxspec.config.source.ConfigSource;
import com.lamprism.luxspec.config.source.ConfigSourceScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author RollW
 */
class LuxspecProcessConfigAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LuxspecProcessConfigAutoConfiguration.class));

    @Test
    void createsProcessBootstrapSources() {
        contextRunner
                .withBean(DefaultApplicationArguments.class, () -> new DefaultApplicationArguments(
                        "--database.type=h2"
                ))
                .run(context -> {
                    assertThat(context).hasBean("environmentConfigSource");
                    assertThat(context).hasBean("systemPropertyConfigSource");
                    assertThat(context).hasBean("commandLineConfigSource");
                    assertThat(context.getBeansOfType(ConfigSource.class)).hasSize(3);
                    ConfigEntry entry = context.getBean(
                            "commandLineConfigSource",
                            ConfigSource.class
                    ).get(ConfigKey.of("database.type"));
                    assertThat(entry.getState()).isEqualTo(ConfigEntry.State.PRESENT);
                    assertThat(entry.requireRawValue().requireString()).isEqualTo("h2");
                });
    }

    @Test
    void readsTheConfiguredTomlFile(@TempDir Path directory) throws IOException {
        Path path = directory.resolve("application.toml");
        Files.writeString(path, "database.type = \"postgresql\"\n");

        contextRunner
                .withPropertyValues("luxspec.config.toml.path=" + path)
                .run(context -> {
                    assertThat(context).hasBean("tomlConfigSource");
                    ConfigSource source = context.getBean(
                            "tomlConfigSource",
                            ConfigSource.class
                    );
                    assertThat(source.getScope()).isEqualTo(ConfigSourceScope.BOOTSTRAP);
                    ConfigEntry entry = source.get(ConfigKey.of("database.type"));
                    assertThat(entry.getState()).isEqualTo(ConfigEntry.State.PRESENT);
                    assertThat(entry.requireRawValue().requireString()).isEqualTo("postgresql");
                });
    }
}
