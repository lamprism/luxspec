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
import com.lamprism.luxspec.config.ConfigSpec;
import com.lamprism.luxspec.config.ConfigValue;
import com.lamprism.luxspec.security.authentication.AccessTokenAuthenticator;
import com.lamprism.luxspec.security.authentication.Subject;
import com.lamprism.luxspec.security.authentication.SubjectResolver;
import com.lamprism.luxspec.security.crypto.DefaultPublicKeyDeriver;
import com.lamprism.luxspec.security.crypto.PublicKeyDeriver;
import com.lamprism.luxspec.security.crypto.config.ConfigKeySetProvider;
import com.lamprism.luxspec.security.jwt.ConfigJwtAccessTokenSettingsSource;
import com.lamprism.luxspec.security.jwt.JwtAccessTokenAdapter;
import com.lamprism.luxspec.security.jwt.JwtAccessTokenConfigSpecs;
import com.lamprism.luxspec.security.jwt.JwtAccessTokenOptions;
import com.lamprism.luxspec.security.token.access.AccessTokenRevocationStore;
import com.lamprism.luxspec.security.token.access.VerifiedAccessToken;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
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
                    assertThat(context).doesNotHaveBean(ConfigJwtAccessTokenSettingsSource.class);
                });
    }

    @Test
    void createsTheDefaultAdapterWithoutRegisteringItsSettingsAdapter() {
        contextRunner
                .withPropertyValues("luxspec.security.jwt.enabled=true")
                .withBean(ConfigReader.class, ConfiguredJwtReader::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(JwtAccessTokenAdapter.class);
                    assertThat(context).doesNotHaveBean(ConfigJwtAccessTokenSettingsSource.class);
                });
    }

    @Test
    void leavesAccessTokenAuthenticationAssemblyToSecurityAutoConfiguration() {
        contextRunner
                .withPropertyValues("luxspec.security.jwt.enabled=true")
                .withBean(ConfigReader.class, EmptyConfigReader::new)
                .withBean(JwtAccessTokenAdapter.class, LuxspecJwtAutoConfigurationTest::accessTokenAdapter)
                .withBean(SubjectResolver.class, UnsupportedSubjectResolver::new)
                .withBean(AccessTokenRevocationStore.class, NeverRevokedAccessTokenStore::new)
                .run(context -> assertThat(context).doesNotHaveBean(AccessTokenAuthenticator.class));
    }

    private static JwtAccessTokenAdapter accessTokenAdapter() {
        return new JwtAccessTokenAdapter(
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
        );
    }

    private static final class EmptyConfigReader implements ConfigReader {
        @Override
        public <T> ConfigValue<T> get(ConfigBinding<T> binding) {
            return ConfigValue.absent();
        }
    }

    private static final class ConfiguredJwtReader implements ConfigReader {
        @Override
        public <T> ConfigValue<T> get(ConfigBinding<T> binding) {
            ConfigSpec<T> spec = binding.getSpec();
            if (JwtAccessTokenConfigSpecs.ACCESS_TTL.equals(spec)) {
                return configured(Duration.ofMinutes(1));
            }
            if (JwtAccessTokenConfigSpecs.ACCESS_ISSUER.equals(spec)) {
                return configured("issuer");
            }
            if (JwtAccessTokenConfigSpecs.ACCESS_AUDIENCES.equals(spec)) {
                return configured(List.of("application"));
            }
            if (JwtAccessTokenConfigSpecs.ACCESS_CLOCK_SKEW.equals(spec)) {
                return configured(Duration.ZERO);
            }
            if (JwtAccessTokenConfigSpecs.KEY_SET_NAME.equals(spec)) {
                return configured("access");
            }
            return ConfigValue.absent();
        }

        @SuppressWarnings("unchecked")
        private static <T> ConfigValue<T> configured(Object value) {
            return (ConfigValue<T>) ConfigValue.defaultValue(value);
        }
    }

    private static final class UnsupportedSubjectResolver implements SubjectResolver {
        @Override
        public Subject resolve(String type, String id) {
            throw new UnsupportedOperationException("Context tests do not resolve subjects");
        }
    }

    private static final class NeverRevokedAccessTokenStore implements AccessTokenRevocationStore {
        @Override
        public void revoke(VerifiedAccessToken accessToken) {
        }

        @Override
        public boolean isRevoked(VerifiedAccessToken accessToken) {
            return false;
        }
    }
}
