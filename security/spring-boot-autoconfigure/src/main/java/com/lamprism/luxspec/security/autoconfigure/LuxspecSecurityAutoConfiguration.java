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

import com.lamprism.luxspec.context.ExecutionContextStorage;
import com.lamprism.luxspec.security.authentication.AccessTokenAuthenticator;
import com.lamprism.luxspec.security.authentication.SubjectResolver;
import com.lamprism.luxspec.security.spring.authentication.AuthorizationHeaderBearerTokenResolver;
import com.lamprism.luxspec.security.spring.authentication.LuxspecAccessTokenAuthenticationProvider;
import com.lamprism.luxspec.security.spring.authentication.LuxspecBearerAuthenticationFilter;
import com.lamprism.luxspec.security.token.access.AccessTokenRevocationStore;
import com.lamprism.luxspec.security.token.access.AccessTokenVerifier;
import com.lamprism.luxspec.security.token.access.InMemoryAccessTokenRevocationStore;
import com.lamprism.luxspec.security.token.refresh.InMemoryRefreshTokenSessionStore;
import com.lamprism.luxspec.security.token.refresh.RefreshTokenSessionStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.SecurityContextHolderFilter;

import java.time.Clock;

/**
 * Configures default access-token authentication and a stateless servlet Bearer chain when an
 * application has not defined one.
 *
 * @author RollW
 */
@AutoConfiguration(
        beforeName = "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration",
        afterName = "com.lamprism.luxspec.security.jwt.autoconfigure.LuxspecJwtAutoConfiguration"
)
public class LuxspecSecurityAutoConfiguration {
    /**
     * Creates the opt-in process-local access-token revocation store.
     *
     * @param clocks optional application clock
     * @return the in-memory revocation store
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "luxspec.security.memory.revocation",
            name = "enabled",
            havingValue = "true"
    )
    @ConditionalOnMissingBean(AccessTokenRevocationStore.class)
    public InMemoryAccessTokenRevocationStore inMemoryAccessTokenRevocationStore(
            ObjectProvider<Clock> clocks
    ) {
        return new InMemoryAccessTokenRevocationStore(clocks.getIfAvailable(Clock::systemUTC));
    }

    /**
     * Creates the opt-in process-local refresh-token session store.
     *
     * @param clocks optional application clock
     * @return the in-memory refresh session store
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "luxspec.security.memory.refresh",
            name = "enabled",
            havingValue = "true"
    )
    @ConditionalOnMissingBean(RefreshTokenSessionStore.class)
    public InMemoryRefreshTokenSessionStore inMemoryRefreshTokenSessionStore(
            ObjectProvider<Clock> clocks
    ) {
        return new InMemoryRefreshTokenSessionStore(clocks.getIfAvailable(Clock::systemUTC));
    }

    /**
     * Creates the provider-neutral access-token authenticator when an application supplies the
     * verification, subject-resolution, and revocation roles.
     *
     * @param tokenVerifier   the access-token verifier
     * @param subjectResolver the current-subject resolver
     * @param revocationStore the verified-token revocation policy
     * @return the access-token authenticator
     */
    @Bean
    @ConditionalOnBean({AccessTokenVerifier.class, SubjectResolver.class, AccessTokenRevocationStore.class})
    @ConditionalOnMissingBean(AccessTokenAuthenticator.class)
    public AccessTokenAuthenticator accessTokenAuthenticator(
            AccessTokenVerifier tokenVerifier,
            SubjectResolver subjectResolver,
            AccessTokenRevocationStore revocationStore
    ) {
        return new AccessTokenAuthenticator(tokenVerifier, subjectResolver, revocationStore);
    }

    /**
     * Servlet-only Spring Security integration.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = Type.SERVLET)
    @ConditionalOnClass({HttpSecurity.class, SecurityContextHolderFilter.class})
    public static class ServletSecurityConfiguration {
        /**
         * Creates the Spring provider for the configured provider-independent Access Token flow.
         *
         * @param authenticator the configured Luxspec Access Token authenticator
         * @return the Spring Security authentication provider
         */
        @ConditionalOnMissingBean
        @ConditionalOnBean(AccessTokenAuthenticator.class)
        @Bean
        public LuxspecAccessTokenAuthenticationProvider luxspecAccessTokenAuthenticationProvider(
                AccessTokenAuthenticator authenticator
        ) {
            return new LuxspecAccessTokenAuthenticationProvider(authenticator);
        }

        /**
         * Creates a default authentication manager for the Luxspec Access Token provider.
         *
         * @param provider the Luxspec Access Token provider
         * @return the default authentication manager
         */
        @ConditionalOnBean(LuxspecAccessTokenAuthenticationProvider.class)
        @ConditionalOnMissingBean(AuthenticationManager.class)
        @Bean
        public AuthenticationManager luxspecAuthenticationManager(
                LuxspecAccessTokenAuthenticationProvider provider
        ) {
            return new ProviderManager(provider);
        }

        /**
         * Creates the protected default chain for an application that supplies access-token authentication.
         *
         * @param httpSecurity          the servlet security builder
         * @param authenticationManager the configured Spring authentication manager
         * @param storage               the optional configured context storage
         * @return the stateless protected filter chain
         */
        @ConditionalOnBean(AccessTokenAuthenticator.class)
        @ConditionalOnMissingBean(SecurityFilterChain.class)
        @Bean
        public SecurityFilterChain luxspecSecurityFilterChain(
                HttpSecurity httpSecurity,
                AuthenticationManager authenticationManager,
                ObjectProvider<ExecutionContextStorage> storage
        ) {
            AuthenticationEntryPoint unauthorized = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
            httpSecurity
                    .csrf(AbstractHttpConfigurer::disable)
                    .formLogin(AbstractHttpConfigurer::disable)
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .logout(AbstractHttpConfigurer::disable)
                    .requestCache(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorized))
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                    .addFilterAfter(
                            new LuxspecBearerAuthenticationFilter(
                                    authenticationManager,
                                    new AuthorizationHeaderBearerTokenResolver(),
                                    unauthorized,
                                    storage.getIfAvailable()
                            ),
                            SecurityContextHolderFilter.class
                    );
            return httpSecurity.build();
        }
    }
}
