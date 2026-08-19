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

import com.lamprism.luxspec.AuthErrorCode;
import com.lamprism.luxspec.audit.AuditActor;
import com.lamprism.luxspec.audit.AuditEntry;
import com.lamprism.luxspec.audit.AuditEventId;
import com.lamprism.luxspec.audit.AuditFieldSet;
import com.lamprism.luxspec.audit.AuditMetadata;
import com.lamprism.luxspec.audit.AuditOutcome;
import com.lamprism.luxspec.audit.integration.config.ConfigAuditFields;
import com.lamprism.luxspec.audit.integration.security.SecurityAuditFields;
import com.lamprism.luxspec.audit.publish.AuditDeliveryPolicy;
import com.lamprism.luxspec.audit.publish.AuditPublisher;
import com.lamprism.luxspec.audit.publish.AuditRegistry;
import com.lamprism.luxspec.audit.publish.DefaultAuditPublisher;
import com.lamprism.luxspec.config.ConfigBinding;
import com.lamprism.luxspec.config.ConfigCodecs;
import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigSpec;
import com.lamprism.luxspec.config.ConfigValue;
import com.lamprism.luxspec.config.event.ConfigChangedEvent;
import com.lamprism.luxspec.config.event.ConfigSourceChangeType;
import com.lamprism.luxspec.config.event.ConfigSourceChangedEvent;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.context.CorrelationId;
import com.lamprism.luxspec.context.ExecutionContext;
import com.lamprism.luxspec.context.ExecutionContextKeys;
import com.lamprism.luxspec.context.ExecutionContexts;
import com.lamprism.luxspec.event.EventDispatcher;
import com.lamprism.luxspec.event.EventDispatcherImpl;
import com.lamprism.luxspec.resource.ResourceReference;
import com.lamprism.luxspec.resource.ResourceType;
import com.lamprism.luxspec.security.SecurityErrorCode;
import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authentication.AuthenticationEvent;
import com.lamprism.luxspec.security.authentication.SecurityContextKeys;
import com.lamprism.luxspec.security.authentication.UserSubject;
import com.lamprism.luxspec.security.authorization.AuthorizationDecision;
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import com.lamprism.luxspec.security.authorization.AuthorizationRequirement;
import com.lamprism.luxspec.security.authorization.ResourceAction;
import com.lamprism.luxspec.security.authorization.ResourceAuthorizationDecisionEvent;
import com.lamprism.luxspec.security.firewall.FirewallRuleDeniedEvent;
import com.lamprism.luxspec.security.firewall.FirewallRuleFailureEvent;
import com.lamprism.luxspec.security.token.IssuedToken;
import com.lamprism.luxspec.security.token.TokenIssuance;
import com.lamprism.luxspec.security.token.TokenLifecycleEvent;
import com.lamprism.luxspec.security.token.access.AccessToken;
import com.lamprism.luxspec.user.Role;
import com.lamprism.luxspec.user.User;
import com.lamprism.luxspec.user.UserStatus;
import com.lamprism.luxspec.user.lifecycle.UserEmailChangedEvent;
import com.lamprism.luxspec.user.lifecycle.UserRegisteredEvent;
import com.lamprism.luxspec.user.lifecycle.UserRenamedEvent;
import com.lamprism.luxspec.user.lifecycle.UserRolesChangedEvent;
import com.lamprism.luxspec.user.lifecycle.UserStatusChangedEvent;
import com.lamprism.luxspec.user.resource.UserResourceTypes;
import com.lamprism.luxspec.user.security.password.UserPasswordChangedEvent;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditEventRegistrationImplTest {
    private static final Instant PUBLISHED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void buildsTheCompleteStandardRegistryAndAllowsApplicationExtensions() {
        AuditRegistry registry = StandardAuditRegistry.register(AuditRegistry.builder())
                .register("application.test", envelope -> null)
                .build();

        Set<String> eventNames = Set.of(
                LuxspecAuditEventNames.CONFIG_SOURCE_CHANGED,
                LuxspecAuditEventNames.CONFIG_EFFECTIVE_CHANGED,
                LuxspecAuditEventNames.SECURITY_AUTHORIZATION_DECIDED,
                LuxspecAuditEventNames.SECURITY_AUTHENTICATION,
                LuxspecAuditEventNames.SECURITY_TOKEN_LIFECYCLE,
                LuxspecAuditEventNames.SECURITY_FIREWALL_RULE_DENIED,
                LuxspecAuditEventNames.SECURITY_FIREWALL_RULE_FAILED,
                LuxspecAuditEventNames.USER_REGISTERED,
                LuxspecAuditEventNames.USER_RENAMED,
                LuxspecAuditEventNames.USER_EMAIL_CHANGED,
                LuxspecAuditEventNames.USER_ROLES_CHANGED,
                LuxspecAuditEventNames.USER_STATUS_CHANGED,
                LuxspecAuditEventNames.USER_PASSWORD_CHANGED,
                "application.test"
        );
        for (String eventName : eventNames) {
            assertNotNull(registry.find(eventName));
        }
    }

    @Test
    void routesEveryExistingAuditableEventToThePublisher() {
        List<AuditEntry> entries = new ArrayList<>();
        AuditPublisher publisher = publisher(entries);
        EventDispatcher dispatcher = new EventDispatcherImpl((event, listener, failure) -> {
            throw new AssertionError("Audit listener failed", failure);
        });

        try (AuditEventRegistration ignored = new AuditEventRegistrationImpl(dispatcher, publisher)) {
            dispatcher.publish(new ConfigSourceChangedEvent(
                    ConfigSourceId.of("environment"),
                    ConfigKey.of("app.mode"),
                    ConfigSourceChangeType.SET
            ));
            dispatcher.publish(new ConfigChangedEvent(
                    binding(),
                    ConfigValue.source("before", ConfigSourceId.of("file")),
                    ConfigValue.source("after", ConfigSourceId.of("environment"))
            ));
            dispatcher.publish(new ResourceAuthorizationDecisionEvent<>(
                    authentication(),
                    ResourceAction.of(ARTICLE, "read", AuthorizationRequirement.none()),
                    new ResourceReference<>(ARTICLE, 42L),
                    AuthorizationDecision.denied(SecurityErrorCode.RESOURCE_ACCESS_DENIED),
                    PUBLISHED_AT,
                    Duration.ofMillis(12L)
            ));
            dispatcher.publish(new FirewallRuleFailureEvent(
                    "example.FirewallRule",
                    SecurityErrorCode.FIREWALL_RULE_FAILURE,
                    PUBLISHED_AT
            ));
            dispatcher.publish(AuthenticationEvent.succeeded(
                    "username-password",
                    authentication(),
                    PUBLISHED_AT,
                    Duration.ofMillis(3L)
            ));
            dispatcher.publish(AuthenticationEvent.failed(
                    "username-password",
                    AuthErrorCode.INVALID_CREDENTIALS,
                    PUBLISHED_AT,
                    Duration.ofMillis(4L)
            ));
            TokenIssuance issuance = TokenIssuance.of(List.of(IssuedToken.of(
                    new AccessToken("redacted-test-token"),
                    PUBLISHED_AT,
                    PUBLISHED_AT.plusSeconds(60L)
            )));
            dispatcher.publish(TokenLifecycleEvent.issued(
                    authentication().subject(),
                    issuance,
                    PUBLISHED_AT,
                    Duration.ofMillis(5L)
            ));
            dispatcher.publish(TokenLifecycleEvent.rejected(
                    TokenLifecycleEvent.Operation.REFRESH,
                    null,
                    AuthErrorCode.REFRESH_TOKEN_REJECTED,
                    PUBLISHED_AT,
                    Duration.ofMillis(6L)
            ));
            dispatcher.publish(new FirewallRuleDeniedEvent(
                    "example.FirewallRule",
                    SecurityErrorCode.FIREWALL_PATH_DENIED,
                    Duration.ofSeconds(1L),
                    PUBLISHED_AT
            ));
            User user = new User(
                    7L,
                    "alice",
                    null,
                    Set.of(Role.USER),
                    UserStatus.ACTIVE,
                    PUBLISHED_AT,
                    PUBLISHED_AT
            );
            dispatcher.publish(new UserRegisteredEvent(user, PUBLISHED_AT));
            dispatcher.publish(new UserRenamedEvent(7L, PUBLISHED_AT));
            dispatcher.publish(new UserEmailChangedEvent(7L, true, PUBLISHED_AT));
            dispatcher.publish(new UserRolesChangedEvent(
                    7L,
                    Role.ADMIN,
                    UserRolesChangedEvent.ChangeType.GRANTED,
                    PUBLISHED_AT
            ));
            dispatcher.publish(new UserStatusChangedEvent(7L, UserStatus.DISABLED, PUBLISHED_AT));
            dispatcher.publish(new UserPasswordChangedEvent(7L, PUBLISHED_AT));
        }

        assertEquals(15, entries.size());
        assertEquals(LuxspecAuditEventNames.CONFIG_SOURCE_CHANGED, entries.get(0).eventName());
        assertEquals(AuditOutcome.SUCCESS, entries.get(0).outcome());
        assertEquals(LuxspecAuditEventNames.CONFIG_EFFECTIVE_CHANGED, entries.get(1).eventName());
        assertTrue(entries.get(1).fields().get(ConfigAuditFields.CONFIG_SENSITIVE).orElseThrow());
        assertEquals(LuxspecAuditEventNames.SECURITY_AUTHORIZATION_DECIDED, entries.get(2).eventName());
        assertEquals(AuditOutcome.DENIED, entries.get(2).outcome());
        assertEquals(LuxspecAuditEventNames.SECURITY_FIREWALL_RULE_FAILED, entries.get(3).eventName());
        assertEquals(AuditOutcome.FAILURE, entries.get(3).outcome());
        assertEquals(LuxspecAuditEventNames.SECURITY_AUTHENTICATION, entries.get(4).eventName());
        assertEquals(AuditOutcome.SUCCESS, entries.get(4).outcome());
        assertEquals(LuxspecAuditEventNames.SECURITY_AUTHENTICATION, entries.get(5).eventName());
        assertEquals(AuditOutcome.FAILURE, entries.get(5).outcome());
        assertEquals(LuxspecAuditEventNames.SECURITY_TOKEN_LIFECYCLE, entries.get(6).eventName());
        assertEquals(AuditOutcome.SUCCESS, entries.get(6).outcome());
        assertEquals(LuxspecAuditEventNames.SECURITY_TOKEN_LIFECYCLE, entries.get(7).eventName());
        assertEquals(AuditOutcome.DENIED, entries.get(7).outcome());
        assertEquals(LuxspecAuditEventNames.SECURITY_FIREWALL_RULE_DENIED, entries.get(8).eventName());
        assertEquals(AuditOutcome.DENIED, entries.get(8).outcome());
        assertEquals(LuxspecAuditEventNames.USER_REGISTERED, entries.get(9).eventName());
        ResourceReference<?> userReference = entries.get(9).resource();
        assertNotNull(userReference);
        assertEquals(UserResourceTypes.USER, userReference.resourceType());
        assertEquals(LuxspecAuditEventNames.USER_RENAMED, entries.get(10).eventName());
        assertEquals(LuxspecAuditEventNames.USER_EMAIL_CHANGED, entries.get(11).eventName());
        assertEquals(LuxspecAuditEventNames.USER_ROLES_CHANGED, entries.get(12).eventName());
        assertEquals(LuxspecAuditEventNames.USER_STATUS_CHANGED, entries.get(13).eventName());
        assertEquals(LuxspecAuditEventNames.USER_PASSWORD_CHANGED, entries.get(14).eventName());
    }

    @Test
    void stopsRoutingAfterTheRegistrationIsClosed() {
        List<AuditEntry> entries = new ArrayList<>();
        AuditPublisher publisher = publisher(entries);
        EventDispatcher dispatcher = new EventDispatcherImpl((event, listener, failure) -> {
            throw new AssertionError("Audit listener failed", failure);
        });
        AuditEventRegistration registration = new AuditEventRegistrationImpl(dispatcher, publisher);

        registration.close();
        dispatcher.publish(new FirewallRuleFailureEvent(
                "example.FirewallRule",
                SecurityErrorCode.FIREWALL_RULE_FAILURE,
                PUBLISHED_AT
        ));

        assertTrue(entries.isEmpty());
        registration.close();
    }

    @Test
    void capturesTheExecutionContextActorAndCorrelationId() {
        Authentication authentication = authentication();
        ExecutionContextAuditMetadataProvider provider = new ExecutionContextAuditMetadataProvider();
        ExecutionContext context = ExecutionContext.empty()
                .with(ExecutionContextKeys.CORRELATION_ID,
                        CorrelationId.of("request-1"))
                .with(SecurityContextKeys.AUTHENTICATION, authentication);

        try (ExecutionContexts.Scope ignored = ExecutionContexts.open(context)) {
            AuditMetadata metadata = provider.capture();
            assertEquals(AuditActor.Kind.USER, metadata.actor().kind());
            assertEquals("7", metadata.actor().id());
            assertEquals(CorrelationId.of("request-1"), metadata.correlationId());
        }
    }

    @Test
    void omitsSubjectFieldsForSubjectlessAuthenticationFailures() {
        List<AuditEntry> entries = new ArrayList<>();
        AuditPublisher publisher = publisher(entries);
        AuthenticationEvent event = AuthenticationEvent.failed(
                "username-password",
                AuthErrorCode.INVALID_CREDENTIALS,
                PUBLISHED_AT,
                Duration.ZERO
        );

        publisher.publish(LuxspecAuditEventNames.SECURITY_AUTHENTICATION, event);

        assertEquals(1, entries.size());
        assertEquals(AuditOutcome.FAILURE, entries.get(0).outcome());
        assertTrue(entries.get(0).fields().get(SecurityAuditFields.SECURITY_SUBJECT_TYPE).isEmpty());
        assertTrue(entries.get(0).fields().get(SecurityAuditFields.SECURITY_SUBJECT_ID).isEmpty());
    }

    private static AuditPublisher publisher(List<AuditEntry> entries) {
        AuditMetadata metadata = new AuditMetadata(
                AuditActor.of(AuditActor.Kind.USER, "7"),
                CorrelationId.of("request-1"),
                AuditFieldSet.empty()
        );
        return new DefaultAuditPublisher(
                StandardAuditRegistry.create(),
                entries::add,
                Clock.fixed(PUBLISHED_AT, ZoneOffset.UTC),
                eventName -> AuditEventId.of(eventName),
                () -> metadata,
                AuditDeliveryPolicy.REQUIRED,
                null
        );
    }

    private static ConfigBinding<String> binding() {
        ConfigSpec<String> spec = ConfigSpec.of(
                "app.secret",
                ConfigCodecs.string(),
                null,
                true
        );
        return spec.bind();
    }

    private static Authentication authentication() {
        return new Authentication(
                new UserSubject(7L),
                AuthorizationGrantSet.of(List.of())
        );
    }

    private static final ResourceType<Long> ARTICLE = ResourceType.of("article", Long.class);
}
