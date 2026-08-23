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

package com.lamprism.luxspec.audit.integration;

import com.lamprism.luxspec.audit.integration.config.ConfigAuditEventNames;
import com.lamprism.luxspec.audit.integration.config.ConfigChangedAuditTranslator;
import com.lamprism.luxspec.audit.integration.config.ConfigSourceChangedAuditTranslator;
import com.lamprism.luxspec.audit.integration.security.AuthenticationAuditTranslator;
import com.lamprism.luxspec.audit.integration.security.FirewallRuleDeniedAuditTranslator;
import com.lamprism.luxspec.audit.integration.security.FirewallRuleFailureAuditTranslator;
import com.lamprism.luxspec.audit.integration.security.ResourceAuthorizationDecisionAuditTranslator;
import com.lamprism.luxspec.audit.integration.security.SecurityAuditEventNames;
import com.lamprism.luxspec.audit.integration.security.TokenLifecycleAuditTranslator;
import com.lamprism.luxspec.audit.integration.user.UserAuditEventNames;
import com.lamprism.luxspec.audit.integration.user.UserEmailChangedAuditTranslator;
import com.lamprism.luxspec.audit.integration.user.UserPasswordChangedAuditTranslator;
import com.lamprism.luxspec.audit.integration.user.UserRegisteredAuditTranslator;
import com.lamprism.luxspec.audit.integration.user.UserRenamedAuditTranslator;
import com.lamprism.luxspec.audit.integration.user.UserRolesChangedAuditTranslator;
import com.lamprism.luxspec.audit.integration.user.UserStatusChangedAuditTranslator;
import com.lamprism.luxspec.audit.publish.AuditEventDefinition;
import com.lamprism.luxspec.audit.publish.AuditEventRegistry;
import com.lamprism.luxspec.audit.publish.AuditPublisher;
import com.lamprism.luxspec.audit.publish.AuditRegistry;
import com.lamprism.luxspec.config.event.ConfigChangedEvent;
import com.lamprism.luxspec.config.event.ConfigSourceChangedEvent;
import com.lamprism.luxspec.event.EventDispatcher;
import com.lamprism.luxspec.security.authentication.AuthenticationEvent;
import com.lamprism.luxspec.security.authorization.ResourceAuthorizationDecisionEvent;
import com.lamprism.luxspec.security.firewall.FirewallRuleDeniedEvent;
import com.lamprism.luxspec.security.firewall.FirewallRuleFailureEvent;
import com.lamprism.luxspec.security.token.TokenLifecycleEvent;
import com.lamprism.luxspec.user.lifecycle.UserEmailChangedEvent;
import com.lamprism.luxspec.user.lifecycle.UserRegisteredEvent;
import com.lamprism.luxspec.user.lifecycle.UserRenamedEvent;
import com.lamprism.luxspec.user.lifecycle.UserRolesChangedEvent;
import com.lamprism.luxspec.user.lifecycle.UserStatusChangedEvent;
import com.lamprism.luxspec.user.security.password.UserPasswordChangedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Catalog of standard event definitions grouped by owning feature.
 *
 * <p>Definitions are initialized lazily by group. A caller that only enables
 * one feature group does not initialize the other groups, and can pass the
 * selected list to {@link AuditEventRegistry#registerAll(Iterable)}.</p>
 *
 * @author RollW
 */
public final class StandardAuditEventCatalog {
    private StandardAuditEventCatalog() {
    }

    /**
     * Returns all standard definitions.
     *
     * @return the standard definitions in stable registration order
     */
    public static List<AuditEventDefinition<?>> all() {
        List<AuditEventDefinition<?>> definitions = new ArrayList<>();
        definitions.addAll(configuration());
        definitions.addAll(security());
        definitions.addAll(users());
        return List.copyOf(definitions);
    }

    /**
     * Returns configuration event definitions.
     *
     * @return configuration definitions
     */
    public static List<AuditEventDefinition<?>> configuration() {
        return ConfigurationDefinitions.ALL;
    }

    /**
     * Returns security event definitions.
     *
     * @return security definitions
     */
    public static List<AuditEventDefinition<?>> security() {
        return SecurityDefinitions.ALL;
    }

    /**
     * Returns user lifecycle event definitions.
     *
     * @return user lifecycle definitions
     */
    public static List<AuditEventDefinition<?>> users() {
        return UserDefinitions.ALL;
    }

    /**
     * Creates a registry containing all standard definitions.
     *
     * @return the immutable standard registry
     */
    public static AuditRegistry createRegistry() {
        return register(AuditRegistry.builder()).build();
    }

    /**
     * Creates an event registry containing every standard definition.
     *
     * @param dispatcher the dispatcher that owns event listeners
     * @param publisher  the publisher that receives translated events
     * @return the registered event registry
     */
    public static AuditEventRegistry createEventRegistry(
            EventDispatcher dispatcher,
            AuditPublisher publisher
    ) {
        return register(new AuditEventRegistry(dispatcher, publisher));
    }

    /**
     * Registers all standard definitions into a builder.
     *
     * @param registry the target builder
     * @return the same builder
     */
    public static AuditRegistry.Builder register(AuditRegistry.Builder registry) {
        return register(registry, all());
    }

    /**
     * Registers a selected set of definitions into a builder.
     *
     * @param registry    the target builder
     * @param definitions the definitions to register
     * @return the same builder
     */
    public static AuditRegistry.Builder register(
            AuditRegistry.Builder registry,
            Iterable<? extends AuditEventDefinition<?>> definitions
    ) {
        AuditRegistry.Builder nonNullRegistry = Objects.requireNonNull(registry, "registry");
        Iterable<? extends AuditEventDefinition<?>> nonNullDefinitions = Objects.requireNonNull(
                definitions,
                "definitions"
        );
        for (AuditEventDefinition<?> definition : nonNullDefinitions) {
            Objects.requireNonNull(definition, "definition").register(nonNullRegistry);
        }
        return nonNullRegistry;
    }

    /**
     * Registers every standard definition with an event registry.
     *
     * @param registry the target event registry
     * @return the same event registry
     */
    public static AuditEventRegistry register(AuditEventRegistry registry) {
        return Objects.requireNonNull(registry, "registry").registerAll(all());
    }

    /**
     * Registers a selected set of definitions with an event registry.
     *
     * @param registry    the target event registry
     * @param definitions the definitions to subscribe
     * @return the same event registry
     */
    public static AuditEventRegistry register(
            AuditEventRegistry registry,
            Iterable<? extends AuditEventDefinition<?>> definitions
    ) {
        return Objects.requireNonNull(registry, "registry").registerAll(definitions);
    }

    private static final class ConfigurationDefinitions {
        private static final List<AuditEventDefinition<?>> ALL = List.of(
                AuditEventDefinition.of(
                        ConfigAuditEventNames.SOURCE_CHANGED,
                        ConfigSourceChangedEvent.class,
                        new ConfigSourceChangedAuditTranslator()
                ),
                AuditEventDefinition.of(
                        ConfigAuditEventNames.EFFECTIVE_CHANGED,
                        ConfigChangedEvent.class,
                        new ConfigChangedAuditTranslator()
                )
        );

        private ConfigurationDefinitions() {
        }
    }

    private static final class SecurityDefinitions {
        @SuppressWarnings({"rawtypes", "unchecked"})
        private static final Class<ResourceAuthorizationDecisionEvent<?>> AUTHORIZATION_EVENT_TYPE =
                (Class) ResourceAuthorizationDecisionEvent.class;

        private static final List<AuditEventDefinition<?>> ALL = List.of(
                AuditEventDefinition.of(
                        SecurityAuditEventNames.AUTHORIZATION_DECIDED,
                        AUTHORIZATION_EVENT_TYPE,
                        new ResourceAuthorizationDecisionAuditTranslator()
                ),
                AuditEventDefinition.of(
                        SecurityAuditEventNames.AUTHENTICATION,
                        AuthenticationEvent.class,
                        new AuthenticationAuditTranslator()
                ),
                AuditEventDefinition.of(
                        SecurityAuditEventNames.TOKEN_LIFECYCLE,
                        TokenLifecycleEvent.class,
                        new TokenLifecycleAuditTranslator()
                ),
                AuditEventDefinition.of(
                        SecurityAuditEventNames.FIREWALL_RULE_DENIED,
                        FirewallRuleDeniedEvent.class,
                        new FirewallRuleDeniedAuditTranslator()
                ),
                AuditEventDefinition.of(
                        SecurityAuditEventNames.FIREWALL_RULE_FAILED,
                        FirewallRuleFailureEvent.class,
                        new FirewallRuleFailureAuditTranslator()
                )
        );

        private SecurityDefinitions() {
        }
    }

    private static final class UserDefinitions {
        private static final List<AuditEventDefinition<?>> ALL = List.of(
                AuditEventDefinition.of(
                        UserAuditEventNames.REGISTERED,
                        UserRegisteredEvent.class,
                        new UserRegisteredAuditTranslator()
                ),
                AuditEventDefinition.of(
                        UserAuditEventNames.RENAMED,
                        UserRenamedEvent.class,
                        new UserRenamedAuditTranslator()
                ),
                AuditEventDefinition.of(
                        UserAuditEventNames.EMAIL_CHANGED,
                        UserEmailChangedEvent.class,
                        new UserEmailChangedAuditTranslator()
                ),
                AuditEventDefinition.of(
                        UserAuditEventNames.ROLES_CHANGED,
                        UserRolesChangedEvent.class,
                        new UserRolesChangedAuditTranslator()
                ),
                AuditEventDefinition.of(
                        UserAuditEventNames.STATUS_CHANGED,
                        UserStatusChangedEvent.class,
                        new UserStatusChangedAuditTranslator()
                ),
                AuditEventDefinition.of(
                        UserAuditEventNames.PASSWORD_CHANGED,
                        UserPasswordChangedEvent.class,
                        new UserPasswordChangedAuditTranslator()
                )
        );

        private UserDefinitions() {
        }
    }
}
