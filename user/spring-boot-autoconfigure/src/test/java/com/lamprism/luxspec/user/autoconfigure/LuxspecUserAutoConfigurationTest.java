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

package com.lamprism.luxspec.user.autoconfigure;

import com.lamprism.luxspec.resource.ResourceReference;
import com.lamprism.luxspec.resource.ResourceType;
import com.lamprism.luxspec.user.User;
import com.lamprism.luxspec.user.resource.InMemoryUserStore;
import com.lamprism.luxspec.user.resource.UserProvider;
import com.lamprism.luxspec.user.resource.UserResourceTypes;
import com.lamprism.luxspec.user.security.authorization.UserRoleGrantResolver;
import com.lamprism.luxspec.user.security.authorization.UserRoleGrantResolverImpl;
import com.lamprism.luxspec.user.security.password.Argon2idPasswordScheme;
import com.lamprism.luxspec.user.security.password.EncodedPassword;
import com.lamprism.luxspec.user.security.password.InMemoryUserPasswordStore;
import com.lamprism.luxspec.user.security.password.PasswordScheme;
import com.lamprism.luxspec.user.spring.LuxspecPasswordEncoder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LuxspecUserAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LuxspecUserAutoConfiguration.class));

    @Test
    void createsTheDefaultSchemeAndSpringBridge() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(PasswordScheme.class);
            assertThat(context).hasSingleBean(PasswordEncoder.class);
            assertThat(context.getBean(PasswordScheme.class)).isInstanceOf(Argon2idPasswordScheme.class);
            assertThat(context.getBean(PasswordEncoder.class)).isInstanceOf(LuxspecPasswordEncoder.class);
        });
    }

    @Test
    void createsTheOptInInMemoryUserServicesAsOneAggregateStore() {
        contextRunner
                .withPropertyValues("luxspec.user.memory.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(InMemoryUserStore.class);
                    assertThat(context).hasSingleBean(UserProvider.class);
                    assertThat(context).hasSingleBean(InMemoryUserPasswordStore.class);
                });
    }

    @Test
    void keepsAnApplicationPasswordSchemeAndBridgesIt() {
        contextRunner
                .withUserConfiguration(CustomPasswordSchemeConfiguration.class)
                .run(context -> {
                    assertThat(context.getBean(PasswordScheme.class))
                            .isInstanceOf(CustomPasswordScheme.class);
                    assertThat(context.getBean(PasswordEncoder.class))
                            .isInstanceOf(LuxspecPasswordEncoder.class);
                    assertThat(context.getBeansOfType(Argon2idPasswordScheme.class)).isEmpty();
                });
    }

    @Test
    void keepsAnApplicationPasswordEncoderAndDoesNotAdaptItInward() {
        contextRunner
                .withUserConfiguration(CustomPasswordEncoderConfiguration.class)
                .run(context -> {
                    assertThat(context.getBean(PasswordScheme.class))
                            .isInstanceOf(Argon2idPasswordScheme.class);
                    assertThat(context.getBean(PasswordEncoder.class))
                            .isInstanceOf(CustomPasswordEncoder.class);
                    assertThat(context.getBeansOfType(LuxspecPasswordEncoder.class)).isEmpty();
                });
    }

    @Test
    void createsTheDefaultRoleGrantResolverForAnApplicationUserProvider() {
        contextRunner
                .withBean(UserProvider.class, UnsupportedUserProvider::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(UserRoleGrantResolver.class);
                    assertThat(context.getBean(UserRoleGrantResolver.class))
                            .isInstanceOf(UserRoleGrantResolverImpl.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomPasswordSchemeConfiguration {
        @Bean
        PasswordScheme passwordScheme() {
            return new CustomPasswordScheme();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomPasswordEncoderConfiguration {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new CustomPasswordEncoder();
        }
    }

    private static final class UnsupportedUserProvider implements UserProvider {
        @Override
        public ResourceType<Long> getResourceType() {
            return UserResourceTypes.USER;
        }

        @Override
        public User provide(ResourceReference<Long> reference) {
            throw new UnsupportedOperationException("Context tests do not resolve users");
        }

        @Override
        public List<User> provide(Collection<ResourceReference<Long>> references) {
            throw new UnsupportedOperationException("Context tests do not resolve users");
        }

        @Override
        public User provideByUsername(String username) {
            throw new UnsupportedOperationException("Context tests do not resolve users");
        }
    }

    private static final class CustomPasswordScheme implements PasswordScheme {
        @Override
        public EncodedPassword encode(CharSequence rawPassword) {
            return new EncodedPassword("custom");
        }

        @Override
        public boolean verify(CharSequence rawPassword, EncodedPassword encodedPassword) {
            return true;
        }

        @Override
        public boolean needsUpgrade(EncodedPassword encodedPassword) {
            return false;
        }
    }

    private static final class CustomPasswordEncoder implements PasswordEncoder {
        @Override
        public String encode(CharSequence rawPassword) {
            return "custom";
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return true;
        }

        @Override
        public boolean upgradeEncoding(String encodedPassword) {
            return false;
        }
    }
}
