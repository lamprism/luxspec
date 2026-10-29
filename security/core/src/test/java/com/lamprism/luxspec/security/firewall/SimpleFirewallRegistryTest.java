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

package com.lamprism.luxspec.security.firewall;

import com.lamprism.luxspec.network.ip.IpAddress;
import com.lamprism.luxspec.network.ip.IpNetwork;
import com.lamprism.luxspec.security.SecurityErrorCode;
import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authentication.UserSubject;
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import com.lamprism.luxspec.security.firewall.rule.ClientAddressFirewallRule;
import com.lamprism.luxspec.security.firewall.rule.RequestMethodFirewallRule;
import com.lamprism.luxspec.security.firewall.rule.RequestPathFirewallRule;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author RollW
 */
class SimpleFirewallRegistryTest {
    private static final class TestRequest implements FirewallRequest {
        @Override
        public String getMethod() {
            return "GET";
        }

        @Override
        public String getPath() {
            return "/test";
        }

        @Override
        public IpAddress getClientAddress() {
            return IpAddress.parse("127.0.0.1");
        }
    }

    @Test
    void publishesOrdinaryFirewallDenialsWithSafePolicyMetadata() {
        List<FirewallRuleDeniedEvent> events = new ArrayList<>();
        SimpleFirewallRegistry<TestRequest> registry = new SimpleFirewallRegistry<>(
                List.of(request -> FirewallDecision.deny(
                        SecurityErrorCode.FIREWALL_PATH_DENIED,
                        Duration.ofSeconds(2L)
                )),
                event -> events.add((FirewallRuleDeniedEvent) event),
                Clock.fixed(Instant.parse("2026-08-09T00:00:00Z"), ZoneOffset.UTC)
        );

        FirewallDecision decision = registry.filter(new TestRequest());

        assertFalse(decision.passed());
        assertEquals(SecurityErrorCode.FIREWALL_PATH_DENIED, decision.getReasonCode());
        assertEquals(1, events.size());
        assertEquals(SecurityErrorCode.FIREWALL_PATH_DENIED, events.get(0).getReasonCode());
        assertEquals(Duration.ofSeconds(2L), events.get(0).getRetryAfter());
        assertTrue(events.get(0).getRuleType().contains(SimpleFirewallRegistryTest.class.getName()));
    }

    @Test
    void treatsObserverFailureAsNonAuthoritative() {
        SimpleFirewallRegistry<TestRequest> registry = new SimpleFirewallRegistry<>(
                List.<Firewall<TestRequest>>of(
                        request -> FirewallDecision.deny(SecurityErrorCode.FIREWALL_PATH_DENIED)
                ),
                event -> {
                    throw new IllegalStateException("event publication failed");
                }
        );

        FirewallDecision decision = registry.filter(new TestRequest());

        assertFalse(decision.passed());
        assertEquals(SecurityErrorCode.FIREWALL_PATH_DENIED, decision.getReasonCode());
    }

    @Test
    void convertsFirewallFailureToDefaultDenial() {
        List<FirewallRuleFailureEvent> events = new ArrayList<>();
        SimpleFirewallRegistry<TestRequest> registry = new SimpleFirewallRegistry<>(
                List.<Firewall<TestRequest>>of(request -> {
                    throw new IllegalStateException("firewall failed");
                }),
                event -> events.add((FirewallRuleFailureEvent) event)
        );

        FirewallDecision decision = registry.filter(new TestRequest());

        assertFalse(decision.passed());
        assertEquals(SecurityErrorCode.FIREWALL_RULE_FAILURE, decision.getReasonCode());
        assertEquals(1, events.size());
    }

    @Test
    void allowsAnExplicitAllowToStopLowerPriorityEvaluation() {
        AtomicBoolean lowerFirewallCalled = new AtomicBoolean();
        Firewall<TestRequest> lowerFirewall = request -> {
            lowerFirewallCalled.set(true);
            return FirewallDecision.deny(SecurityErrorCode.FIREWALL_PATH_DENIED);
        };
        Firewall<TestRequest> allowFirewall = new Firewall<>() {
            @Override
            public FirewallDecision verifyRequest(TestRequest request) {
                return FirewallDecision.allow();
            }

            @Override
            public int getPriority() {
                return 10;
            }
        };

        SimpleFirewallRegistry<TestRequest> registry = new SimpleFirewallRegistry<>(
                List.of(lowerFirewall, allowFirewall)
        );

        FirewallDecision decision = registry.filter(new TestRequest());

        assertEquals(FirewallDecision.Case.ALLOW, decision.getCase());
        assertFalse(lowerFirewallCalled.get());
    }

    @Test
    void supportsDenialPriorityAndReportsObserverFailure() {
        List<Throwable> failures = new ArrayList<>();
        IllegalStateException publicationFailure = new IllegalStateException("event publication failed");
        Firewall<TestRequest> allow = new Firewall<>() {
            @Override
            public FirewallDecision verifyRequest(TestRequest request) {
                return FirewallDecision.allow();
            }

            @Override
            public int getPriority() {
                return 10;
            }
        };
        SimpleFirewallRegistry<TestRequest> registry = new SimpleFirewallRegistry<>(
                List.of(allow, request -> FirewallDecision.deny(SecurityErrorCode.FIREWALL_PATH_DENIED)),
                event -> {
                    throw publicationFailure;
                },
                Clock.systemUTC(),
                FirewallDecisionPolicy.denyOverrides(),
                (event, failure) -> failures.add(failure)
        );

        FirewallDecision decision = registry.filter(new TestRequest());

        assertEquals(FirewallDecision.Case.DENY, decision.getCase());
        assertEquals(SecurityErrorCode.FIREWALL_PATH_DENIED, decision.getReasonCode());
        assertEquals(List.of(publicationFailure), failures);
    }

    @Test
    void evaluatesNeutralFirewallsByDescendingPriority() {
        List<Integer> order = new ArrayList<>();
        Firewall<TestRequest> low = firewall(1, order);
        Firewall<TestRequest> high = firewall(10, order);

        SimpleFirewallRegistry<TestRequest> registry = new SimpleFirewallRegistry<>(List.of(low, high));

        assertEquals(FirewallDecision.Case.NEUTRAL, registry.filter(new TestRequest()).getCase());
        assertEquals(List.of(10, 1), order);
    }

    @Test
    void supportsDynamicRegistrationAndRemoval() {
        SimpleFirewallRegistry<TestRequest> registry = new SimpleFirewallRegistry<>(List.of());
        Firewall<TestRequest> denyFirewall = request -> FirewallDecision.deny(
                SecurityErrorCode.FIREWALL_PATH_DENIED
        );

        assertEquals(FirewallDecision.Case.NEUTRAL, registry.filter(new TestRequest()).getCase());
        registry.addFirewall(denyFirewall);
        assertEquals(FirewallDecision.Case.DENY, registry.filter(new TestRequest()).getCase());
        assertTrue(registry.removeFirewall(denyFirewall));
        assertFalse(registry.removeFirewall(denyFirewall));
        assertEquals(FirewallDecision.Case.NEUTRAL, registry.filter(new TestRequest()).getCase());
    }

    @Test
    void appliesCommonRequestFirewallsToAuthenticatedRequests() {
        SimpleFirewallRegistry<AuthenticatedRequest> registry = new SimpleFirewallRegistry<>(
                List.of(
                        RequestMethodFirewallRule.allowOnly(List.of("GET")),
                        RequestPathFirewallRule.allowExact(List.of("/test")),
                        ClientAddressFirewallRule.allowOnly(List.of(IpNetwork.parse("127.0.0.0/8")))
                )
        );
        IngressRequest ingress = new IngressRequest("GET", "/test", IpAddress.parse("127.0.0.1"));
        Authentication authentication = new Authentication(
                new UserSubject(1L),
                AuthorizationGrantSet.of(List.of())
        );

        assertEquals(
                FirewallDecision.Case.ALLOW,
                registry.filter(new AuthenticatedRequest(ingress, authentication)).getCase()
        );
    }

    private static Firewall<TestRequest> firewall(int priority, List<Integer> order) {
        return new Firewall<>() {
            @Override
            public FirewallDecision verifyRequest(TestRequest request) {
                order.add(priority);
                return FirewallDecision.neutral();
            }

            @Override
            public int getPriority() {
                return priority;
            }
        };
    }
}
