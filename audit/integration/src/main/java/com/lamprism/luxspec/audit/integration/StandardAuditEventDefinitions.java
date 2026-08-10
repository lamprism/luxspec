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

import com.lamprism.luxspec.audit.integration.config.ConfigChangedAuditTranslator;
import com.lamprism.luxspec.audit.integration.config.ConfigSourceChangedAuditTranslator;
import com.lamprism.luxspec.audit.integration.security.AuthenticationAuditTranslator;
import com.lamprism.luxspec.audit.integration.security.FirewallRuleDeniedAuditTranslator;
import com.lamprism.luxspec.audit.integration.security.FirewallRuleFailureAuditTranslator;
import com.lamprism.luxspec.audit.integration.security.ResourceAuthorizationDecisionAuditTranslator;
import com.lamprism.luxspec.audit.integration.security.TokenLifecycleAuditTranslator;
import com.lamprism.luxspec.audit.integration.user.UserEmailChangedAuditTranslator;
import com.lamprism.luxspec.audit.integration.user.UserPasswordChangedAuditTranslator;
import com.lamprism.luxspec.audit.integration.user.UserRegisteredAuditTranslator;
import com.lamprism.luxspec.audit.integration.user.UserRenamedAuditTranslator;
import com.lamprism.luxspec.audit.integration.user.UserRolesChangedAuditTranslator;
import com.lamprism.luxspec.audit.integration.user.UserStatusChangedAuditTranslator;
import com.lamprism.luxspec.config.event.ConfigChangedEvent;
import com.lamprism.luxspec.config.event.ConfigSourceChangedEvent;
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

import java.util.List;

/**
 * The single source of truth for standard event type, name, and translator
 * associations.
 *
 * @author RollW
 */
final class StandardAuditEventDefinitions {
    private static final StandardAuditEventDefinition<ConfigSourceChangedEvent> CONFIG_SOURCE_CHANGED =
            StandardAuditEventDefinition.of(
                    LuxspecAuditEventNames.CONFIG_SOURCE_CHANGED,
                    ConfigSourceChangedEvent.class,
                    new ConfigSourceChangedAuditTranslator()
            );
    private static final StandardAuditEventDefinition<ConfigChangedEvent> CONFIG_EFFECTIVE_CHANGED =
            StandardAuditEventDefinition.of(
                    LuxspecAuditEventNames.CONFIG_EFFECTIVE_CHANGED,
                    ConfigChangedEvent.class,
                    new ConfigChangedAuditTranslator()
            );
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final Class<ResourceAuthorizationDecisionEvent<?>> AUTHORIZATION_EVENT_TYPE =
            (Class) ResourceAuthorizationDecisionEvent.class;
    private static final StandardAuditEventDefinition<ResourceAuthorizationDecisionEvent<?>> AUTHORIZATION_DECIDED =
            StandardAuditEventDefinition.of(
                    LuxspecAuditEventNames.SECURITY_AUTHORIZATION_DECIDED,
                    AUTHORIZATION_EVENT_TYPE,
                    new ResourceAuthorizationDecisionAuditTranslator()
            );
    private static final StandardAuditEventDefinition<AuthenticationEvent> AUTHENTICATION =
            StandardAuditEventDefinition.of(
                    LuxspecAuditEventNames.SECURITY_AUTHENTICATION,
                    AuthenticationEvent.class,
                    new AuthenticationAuditTranslator()
            );
    private static final StandardAuditEventDefinition<TokenLifecycleEvent> TOKEN_LIFECYCLE =
            StandardAuditEventDefinition.of(
                    LuxspecAuditEventNames.SECURITY_TOKEN_LIFECYCLE,
                    TokenLifecycleEvent.class,
                    new TokenLifecycleAuditTranslator()
            );
    private static final StandardAuditEventDefinition<FirewallRuleDeniedEvent> FIREWALL_RULE_DENIED =
            StandardAuditEventDefinition.of(
                    LuxspecAuditEventNames.SECURITY_FIREWALL_RULE_DENIED,
                    FirewallRuleDeniedEvent.class,
                    new FirewallRuleDeniedAuditTranslator()
            );
    private static final StandardAuditEventDefinition<FirewallRuleFailureEvent> FIREWALL_RULE_FAILED =
            StandardAuditEventDefinition.of(
                    LuxspecAuditEventNames.SECURITY_FIREWALL_RULE_FAILED,
                    FirewallRuleFailureEvent.class,
                    new FirewallRuleFailureAuditTranslator()
            );
    private static final StandardAuditEventDefinition<UserRegisteredEvent> USER_REGISTERED =
            StandardAuditEventDefinition.of(
                    LuxspecAuditEventNames.USER_REGISTERED,
                    UserRegisteredEvent.class,
                    new UserRegisteredAuditTranslator()
            );
    private static final StandardAuditEventDefinition<UserRenamedEvent> USER_RENAMED =
            StandardAuditEventDefinition.of(
                    LuxspecAuditEventNames.USER_RENAMED,
                    UserRenamedEvent.class,
                    new UserRenamedAuditTranslator()
            );
    private static final StandardAuditEventDefinition<UserEmailChangedEvent> USER_EMAIL_CHANGED =
            StandardAuditEventDefinition.of(
                    LuxspecAuditEventNames.USER_EMAIL_CHANGED,
                    UserEmailChangedEvent.class,
                    new UserEmailChangedAuditTranslator()
            );
    private static final StandardAuditEventDefinition<UserRolesChangedEvent> USER_ROLES_CHANGED =
            StandardAuditEventDefinition.of(
                    LuxspecAuditEventNames.USER_ROLES_CHANGED,
                    UserRolesChangedEvent.class,
                    new UserRolesChangedAuditTranslator()
            );
    private static final StandardAuditEventDefinition<UserStatusChangedEvent> USER_STATUS_CHANGED =
            StandardAuditEventDefinition.of(
                    LuxspecAuditEventNames.USER_STATUS_CHANGED,
                    UserStatusChangedEvent.class,
                    new UserStatusChangedAuditTranslator()
            );
    private static final StandardAuditEventDefinition<UserPasswordChangedEvent> USER_PASSWORD_CHANGED =
            StandardAuditEventDefinition.of(
                    LuxspecAuditEventNames.USER_PASSWORD_CHANGED,
                    UserPasswordChangedEvent.class,
                    new UserPasswordChangedAuditTranslator()
            );

    private static final List<StandardAuditEventDefinition<?>> ALL = List.of(
            CONFIG_SOURCE_CHANGED,
            CONFIG_EFFECTIVE_CHANGED,
            AUTHORIZATION_DECIDED,
            AUTHENTICATION,
            TOKEN_LIFECYCLE,
            FIREWALL_RULE_DENIED,
            FIREWALL_RULE_FAILED,
            USER_REGISTERED,
            USER_RENAMED,
            USER_EMAIL_CHANGED,
            USER_ROLES_CHANGED,
            USER_STATUS_CHANGED,
            USER_PASSWORD_CHANGED
    );

    private StandardAuditEventDefinitions() {
    }

    static List<StandardAuditEventDefinition<?>> all() {
        return ALL;
    }
}
