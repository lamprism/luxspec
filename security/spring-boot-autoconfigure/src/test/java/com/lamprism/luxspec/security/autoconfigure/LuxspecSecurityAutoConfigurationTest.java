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

package com.lamprism.luxspec.security.autoconfigure;

import com.lamprism.luxspec.security.authentication.AccessTokenAuthenticator;
import com.lamprism.luxspec.security.authentication.Subject;
import com.lamprism.luxspec.security.authentication.SubjectResolver;
import com.lamprism.luxspec.security.spring.authentication.LuxspecBearerAuthenticationFilter;
import com.lamprism.luxspec.security.token.TokenVerifier;
import com.lamprism.luxspec.security.token.access.AccessToken;
import com.lamprism.luxspec.security.token.access.VerifiedAccessToken;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CsrfFilter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LuxspecSecurityAutoConfigurationTest {
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ServletWebSecurityAutoConfiguration.class,
                    LuxspecSecurityAutoConfiguration.class
            ));

    @Test
    void createsAStatelessBearerChainWhenAnAccessTokenAuthenticatorIsAvailable() {
        contextRunner
                .withUserConfiguration(AccessTokenAuthenticatorConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(SecurityFilterChain.class);
                    assertThat(context.getBeansOfType(LuxspecBearerAuthenticationFilter.class)).isEmpty();
                    SecurityFilterChain configuredChain = context.getBean(SecurityFilterChain.class);
                    assertThat(configuredChain).isInstanceOf(DefaultSecurityFilterChain.class);
                    DefaultSecurityFilterChain filterChain = (DefaultSecurityFilterChain) configuredChain;
                    List<Filter> filters = filterChain.getFilters();

                    assertThat(indexOf(filters, LuxspecBearerAuthenticationFilter.class))
                            .isGreaterThan(indexOf(filters, SecurityContextHolderFilter.class));
                    assertThat(filters).noneMatch(CsrfFilter.class::isInstance);
                });
    }

    @Test
    void backsOffWhenAnApplicationDefinesItsOwnFilterChain() {
        contextRunner
                .withUserConfiguration(
                        AccessTokenAuthenticatorConfiguration.class,
                        CustomSecurityFilterChainConfiguration.class
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(SecurityFilterChain.class);
                    SecurityFilterChain configuredChain = context.getBean(SecurityFilterChain.class);
                    DefaultSecurityFilterChain filterChain = (DefaultSecurityFilterChain) configuredChain;

                    assertThat(filterChain.getFilters()).noneMatch(LuxspecBearerAuthenticationFilter.class::isInstance);
                });
    }

    @Test
    void doesNotInstallTheLuxspecFilterWithoutAnAccessTokenAuthenticator() {
        contextRunner.run(context -> {
            SecurityFilterChain configuredChain = context.getBean(SecurityFilterChain.class);
            DefaultSecurityFilterChain filterChain = (DefaultSecurityFilterChain) configuredChain;

            assertThat(filterChain.getFilters()).noneMatch(LuxspecBearerAuthenticationFilter.class::isInstance);
        });
    }

    private static int indexOf(List<Filter> filters, Class<? extends Filter> filterType) {
        for (int index = 0; index < filters.size(); index++) {
            if (filterType.isInstance(filters.get(index))) {
                return index;
            }
        }
        return -1;
    }

    @Configuration(proxyBeanMethods = false)
    static class AccessTokenAuthenticatorConfiguration {
        @Bean
        AccessTokenAuthenticator accessTokenAuthenticator() {
            return new AccessTokenAuthenticator(
                    new UnsupportedTokenVerifier(),
                    new UnsupportedSubjectResolver()
            );
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomSecurityFilterChainConfiguration {
        @Bean
        SecurityFilterChain customSecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
            httpSecurity.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
            return httpSecurity.build();
        }
    }

    private static final class UnsupportedTokenVerifier
            implements TokenVerifier<AccessToken, VerifiedAccessToken> {
        @Override
        public VerifiedAccessToken verify(AccessToken accessToken) {
            throw new UnsupportedOperationException("Context tests do not verify access tokens");
        }
    }

    private static final class UnsupportedSubjectResolver implements SubjectResolver {
        @Override
        public Subject resolve(String subjectType, String subjectId) {
            throw new UnsupportedOperationException("Context tests do not resolve subjects");
        }
    }
}
