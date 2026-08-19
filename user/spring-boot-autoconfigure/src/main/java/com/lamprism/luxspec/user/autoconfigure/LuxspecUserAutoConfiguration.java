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

import com.lamprism.luxspec.security.authentication.SubjectResolver;
import com.lamprism.luxspec.security.authorization.AuthorizationScopeHierarchy;
import com.lamprism.luxspec.user.query.UserBrowser;
import com.lamprism.luxspec.user.resource.InMemoryUserStore;
import com.lamprism.luxspec.user.resource.UserProvider;
import com.lamprism.luxspec.user.resource.UserRegistry;
import com.lamprism.luxspec.user.security.authentication.PasswordAuthenticator;
import com.lamprism.luxspec.user.security.authentication.UserSubjectResolver;
import com.lamprism.luxspec.user.security.authorization.AuthorizationProfileContributor;
import com.lamprism.luxspec.user.security.authorization.UserAuthorizationProfiles;
import com.lamprism.luxspec.user.security.authorization.UserRoleGrantResolver;
import com.lamprism.luxspec.user.security.authorization.UserRoleGrantResolverImpl;
import com.lamprism.luxspec.user.security.password.Argon2idPasswordScheme;
import com.lamprism.luxspec.user.security.password.InMemoryUserPasswordStore;
import com.lamprism.luxspec.user.security.password.PasswordScheme;
import com.lamprism.luxspec.user.security.password.UserPasswordStore;
import com.lamprism.luxspec.user.spring.LuxspecPasswordEncoder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * Supplies the default provider-independent user password protection and Spring bridge.
 *
 * @author RollW
 */
@AutoConfiguration
@ConditionalOnClass({PasswordScheme.class, PasswordEncoder.class})
public class LuxspecUserAutoConfiguration {
    /**
     * Creates the opt-in aggregate in-memory user service.
     *
     * <p>The store is not enabled by default because its state is process-local and volatile. One
     * bean supplies the registry, provider, and browser roles together.</p>
     *
     * @param clocks optional application clock
     * @return the in-memory user service
     */
    @Bean
    @ConditionalOnProperty(prefix = "luxspec.user.memory", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean({UserRegistry.class, UserProvider.class, UserBrowser.class})
    public InMemoryUserStore inMemoryUserStore(ObjectProvider<Clock> clocks) {
        return new InMemoryUserStore(clocks.getIfAvailable(Clock::systemUTC));
    }

    /**
     * Creates the opt-in in-memory protected-password store.
     *
     * @return the in-memory password store
     */
    @Bean
    @ConditionalOnProperty(prefix = "luxspec.user.memory", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(UserPasswordStore.class)
    public InMemoryUserPasswordStore inMemoryUserPasswordStore() {
        return new InMemoryUserPasswordStore();
    }

    /**
     * Creates the default Argon2id password scheme when an application has not supplied one.
     *
     * @return the default password scheme
     */
    @Bean
    @ConditionalOnMissingBean(PasswordScheme.class)
    public PasswordScheme passwordScheme() {
        return new Argon2idPasswordScheme();
    }

    /**
     * Creates the Spring Security bridge when an application has not supplied an encoder.
     *
     * @param passwordScheme the configured authoritative password scheme
     * @return the Spring password encoder bridge
     */
    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder(PasswordScheme passwordScheme) {
        return new LuxspecPasswordEncoder(passwordScheme);
    }

    /**
     * Creates the empty scope hierarchy used when no application hierarchy is supplied.
     *
     * @return the empty scope hierarchy
     */
    @Bean
    @ConditionalOnBean(UserProvider.class)
    @ConditionalOnMissingBean(AuthorizationScopeHierarchy.class)
    public AuthorizationScopeHierarchy authorizationScopeHierarchy() {
        return AuthorizationScopeHierarchy.empty();
    }

    /**
     * Creates the default user role grant resolver when user lookup is available.
     *
     * @param scopeHierarchy the configured scope hierarchy
     * @param contributors   application-defined profile contributors
     * @return the role grant resolver
     */
    @Bean
    @ConditionalOnBean(UserProvider.class)
    @ConditionalOnMissingBean(UserRoleGrantResolver.class)
    public UserRoleGrantResolver userRoleGrantResolver(
            AuthorizationScopeHierarchy scopeHierarchy,
            ObjectProvider<AuthorizationProfileContributor> contributors
    ) {
        List<AuthorizationProfileContributor> allContributors = new ArrayList<>();
        allContributors.add(UserAuthorizationProfiles.defaults());
        contributors.orderedStream().forEach(allContributors::add);
        return new UserRoleGrantResolverImpl(
                UserAuthorizationProfiles.defaultRoleProfiles(),
                allContributors,
                scopeHierarchy
        );
    }

    /**
     * Creates the user subject resolver when an authoritative provider is available.
     *
     * @param userProvider the configured user provider
     * @return the subject resolver
     */
    @Bean
    @ConditionalOnBean(UserProvider.class)
    @ConditionalOnMissingBean(SubjectResolver.class)
    public UserSubjectResolver userSubjectResolver(UserProvider userProvider) {
        return new UserSubjectResolver(userProvider);
    }

    /**
     * Creates username-password authentication when all required roles are available.
     *
     * @param userProvider   the configured user provider
     * @param passwordStore  the configured password store
     * @param passwordScheme the configured password scheme
     * @param grantResolver  the configured role grant resolver
     * @return the password authenticator
     */
    @Bean
    @ConditionalOnBean({UserProvider.class, UserPasswordStore.class, PasswordScheme.class, UserRoleGrantResolver.class})
    @ConditionalOnMissingBean(PasswordAuthenticator.class)
    public PasswordAuthenticator passwordAuthenticator(
            UserProvider userProvider,
            UserPasswordStore passwordStore,
            PasswordScheme passwordScheme,
            UserRoleGrantResolver grantResolver
    ) {
        return new PasswordAuthenticator(
                userProvider,
                passwordStore,
                passwordScheme,
                grantResolver
        );
    }
}
