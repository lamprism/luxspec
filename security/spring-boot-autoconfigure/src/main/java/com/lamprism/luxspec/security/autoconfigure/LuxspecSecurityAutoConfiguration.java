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
import com.lamprism.luxspec.security.spring.authentication.LuxspecAccessTokenAuthenticationProvider;
import com.lamprism.luxspec.security.spring.authentication.LuxspecBearerAuthenticationFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
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

/**
 * Configures a stateless Luxspec Bearer security chain only when an application has not defined one.
 *
 * @author RollW
 */
@AutoConfiguration(beforeName = "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration")
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({HttpSecurity.class, SecurityContextHolderFilter.class})
@ConditionalOnBean(AccessTokenAuthenticator.class)
public class LuxspecSecurityAutoConfiguration {
    /**
     * Creates the Spring provider for the configured provider-independent Access Token flow.
     *
     * @param authenticator the configured Luxspec Access Token authenticator
     * @return the Spring Security authentication provider
     */
    @ConditionalOnMissingBean
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
     * @return the stateless protected filter chain
     */
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    @Bean
    public SecurityFilterChain luxspecSecurityFilterChain(
            HttpSecurity httpSecurity,
            AuthenticationManager authenticationManager
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
                        new LuxspecBearerAuthenticationFilter(authenticationManager),
                        SecurityContextHolderFilter.class
                );
        return httpSecurity.build();
    }
}
