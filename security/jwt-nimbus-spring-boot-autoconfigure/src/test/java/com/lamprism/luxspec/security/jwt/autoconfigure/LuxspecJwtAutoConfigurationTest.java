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

package com.lamprism.luxspec.security.jwt.autoconfigure;

import com.lamprism.luxspec.config.ConfigBinding;
import com.lamprism.luxspec.config.ConfigReader;
import com.lamprism.luxspec.config.ConfigValue;
import com.lamprism.luxspec.security.crypto.DefaultPublicKeyDeriver;
import com.lamprism.luxspec.security.crypto.PublicKeyDeriver;
import com.lamprism.luxspec.security.crypto.config.ConfigKeySetProvider;
import com.lamprism.luxspec.security.jwt.ConfigJwtAccessTokenSettingsSource;
import com.lamprism.luxspec.security.jwt.JwtAccessTokenAdapter;
import com.lamprism.luxspec.security.jwt.JwtAccessTokenOptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;
import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LuxspecJwtAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LuxspecJwtAutoConfiguration.class));

    @Test
    void remainsDisabledByDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(JwtAccessTokenAdapter.class);
            assertThat(context).doesNotHaveBean(ConfigJwtAccessTokenSettingsSource.class);
        });
    }

    @Test
    void waitsForConfigReaderWhenExplicitlyEnabled() {
        contextRunner
                .withPropertyValues("luxspec.security.jwt.enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(JwtAccessTokenAdapter.class);
                    assertThat(context).doesNotHaveBean(ConfigJwtAccessTokenSettingsSource.class);
                });
    }

    @Test
    void createsDefaultKeyMaterialDeriverWhenConfigReaderIsAvailable() {
        contextRunner
                .withPropertyValues("luxspec.security.jwt.enabled=true")
                .withBean(ConfigReader.class, EmptyConfigReader::new)
                .withBean(JwtAccessTokenAdapter.class, () -> new JwtAccessTokenAdapter(
                        keySetName -> {
                            throw new AssertionError("Key-set provider must not be called");
                        },
                        "access",
                        Clock.systemUTC(),
                        new JwtAccessTokenOptions(
                                Duration.ofMinutes(1),
                                "issuer",
                                Set.of(),
                                Duration.ZERO
                        )
                ))
                .run(context -> {
                    assertThat(context).hasSingleBean(PublicKeyDeriver.class);
                    assertThat(context.getBean(PublicKeyDeriver.class))
                            .isInstanceOf(DefaultPublicKeyDeriver.class);
                    assertThat(context).hasSingleBean(ConfigKeySetProvider.class);
                });
    }

    private static final class EmptyConfigReader implements ConfigReader {
        @Override
        public <T> ConfigValue<T> get(ConfigBinding<T> binding) {
            return ConfigValue.absent();
        }
    }
}
