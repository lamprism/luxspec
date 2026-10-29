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

import com.lamprism.luxspec.AuthErrorCode;
import com.lamprism.luxspec.security.authentication.AccessTokenAuthenticator;
import com.lamprism.luxspec.security.authentication.AuthenticationException;
import com.lamprism.luxspec.security.authentication.Subject;
import com.lamprism.luxspec.security.authentication.SubjectResolver;
import com.lamprism.luxspec.security.spring.authentication.BearerTokenResolver;
import com.lamprism.luxspec.security.spring.authentication.LuxspecBearerAuthenticationFilter;
import com.lamprism.luxspec.security.token.TokenAuthenticationPolicy;
import com.lamprism.luxspec.security.token.access.AccessToken;
import com.lamprism.luxspec.security.token.access.AccessTokenRevocationStore;
import com.lamprism.luxspec.security.token.access.AccessTokenVerifier;
import com.lamprism.luxspec.security.token.access.InMemoryAccessTokenRevocationStore;
import com.lamprism.luxspec.security.token.access.VerifiedAccessToken;
import com.lamprism.luxspec.security.token.refresh.InMemoryRefreshTokenSessionStore;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CsrfFilter;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 *
 * @author RollW
 */
class LuxspecSecurityAutoConfigurationTest {
    private final ApplicationContextRunner runtimeContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LuxspecSecurityAutoConfiguration.class));

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ServletWebSecurityAutoConfiguration.class,
                    LuxspecSecurityAutoConfiguration.class
            ));

    @Test
    void createsTheDefaultAuthenticatorWhenAllRuntimeRolesAreAvailable() {
        runtimeContextRunner
                .withBean(AccessTokenVerifier.class, UnsupportedTokenVerifier::new)
                .withBean(SubjectResolver.class, UnsupportedSubjectResolver::new)
                .withBean(AccessTokenRevocationStore.class, UnsupportedAccessTokenRevocationStore::new)
                .run(context -> assertThat(context).hasSingleBean(AccessTokenAuthenticator.class));
    }

    @Test
    void createsAnAuthenticatorFromAnExplicitPolicyWithoutASubjectResolver() {
        runtimeContextRunner
                .withBean(AccessTokenVerifier.class, UnsupportedTokenVerifier::new)
                .withBean(AccessTokenRevocationStore.class, UnsupportedAccessTokenRevocationStore::new)
                .withBean(TokenAuthenticationPolicy.class, () -> (subject, grants) -> {
                    throw new UnsupportedOperationException("Context tests do not resolve authentication");
                })
                .run(context -> assertThat(context).hasSingleBean(AccessTokenAuthenticator.class));
    }

    @Test
    void createsOptInInMemoryTokenStoresOnlyWhenSelected() {
        runtimeContextRunner
                .withPropertyValues(
                        "luxspec.security.memory.revocation.enabled=true",
                        "luxspec.security.memory.refresh.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(InMemoryAccessTokenRevocationStore.class);
                    assertThat(context).hasSingleBean(InMemoryRefreshTokenSessionStore.class);
                });
    }

    @Test
    void doesNotCreateTheDefaultAuthenticatorWithoutARevocationStore() {
        runtimeContextRunner
                .withBean(AccessTokenVerifier.class, UnsupportedTokenVerifier::new)
                .withBean(SubjectResolver.class, UnsupportedSubjectResolver::new)
                .run(context -> assertThat(context).doesNotHaveBean(AccessTokenAuthenticator.class));
    }

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
    void usesApplicationTokenResolverAndAuthenticationEntryPoint() {
        contextRunner
                .withUserConfiguration(AccessTokenAuthenticatorConfiguration.class)
                .withBean(BearerTokenResolver.class, () -> request -> {
                    throw new AuthenticationException(AuthErrorCode.INVALID_TOKEN, "Rejected by custom resolver");
                })
                .withBean(AuthenticationEntryPoint.class, () -> (request, response, failure) -> response.setStatus(418))
                .run(context -> {
                    DefaultSecurityFilterChain chain = (DefaultSecurityFilterChain) context.getBean(SecurityFilterChain.class);
                    LuxspecBearerAuthenticationFilter filter = chain.getFilters().stream()
                            .filter(LuxspecBearerAuthenticationFilter.class::isInstance)
                            .map(LuxspecBearerAuthenticationFilter.class::cast)
                            .findFirst().orElseThrow();
                    MockHttpServletResponse response = new MockHttpServletResponse();
                    assertThatCode(() -> filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain()))
                            .doesNotThrowAnyException();
                    assertThat(response.getStatus()).isEqualTo(418);
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
                    TokenAuthenticationPolicy.issuedGrants(new UnsupportedSubjectResolver()),
                    new UnsupportedAccessTokenRevocationStore()
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

    private static final class UnsupportedTokenVerifier implements AccessTokenVerifier {
        @Override
        public VerifiedAccessToken verify(AccessToken accessToken) {
            throw new UnsupportedOperationException("Context tests do not verify access tokens");
        }

        @Override
        public Instant getAcceptanceDeadline(VerifiedAccessToken accessToken) {
            throw new UnsupportedOperationException("Context tests do not calculate acceptance deadlines");
        }
    }

    private static final class UnsupportedSubjectResolver implements SubjectResolver {
        @Override
        public Subject resolve(String type, String id) {
            throw new UnsupportedOperationException("Context tests do not resolve subjects");
        }
    }

    private static final class UnsupportedAccessTokenRevocationStore implements AccessTokenRevocationStore {
        @Override
        public void revoke(VerifiedAccessToken accessToken, Instant acceptanceDeadline) {
            throw new UnsupportedOperationException("Context tests do not revoke access tokens");
        }

        @Override
        public boolean isRevoked(VerifiedAccessToken accessToken) {
            throw new UnsupportedOperationException("Context tests do not check access token revocation");
        }
    }
}
