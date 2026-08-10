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

import com.lamprism.luxspec.security.SecurityErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FirewallChainTest {
    @Test
    void publishesOrdinaryRuleDenialsWithSafePolicyMetadata() {
        List<FirewallRuleDeniedEvent> events = new ArrayList<>();
        FirewallChain<String> chain = new FirewallChain<>(
                List.of(request -> FirewallDecision.deny(
                        SecurityErrorCode.FIREWALL_PATH_DENIED,
                        Duration.ofSeconds(2L)
                )),
                event -> events.add((FirewallRuleDeniedEvent) event),
                Clock.fixed(Instant.parse("2026-08-09T00:00:00Z"), ZoneOffset.UTC)
        );

        FirewallDecision decision = chain.evaluate("request-facts");

        assertFalse(decision.passed());
        assertEquals(SecurityErrorCode.FIREWALL_PATH_DENIED, decision.getReasonCode());
        assertEquals(1, events.size());
        assertEquals(SecurityErrorCode.FIREWALL_PATH_DENIED, events.get(0).getReasonCode());
        assertEquals(Duration.ofSeconds(2L), events.get(0).getRetryAfter());
        assertTrue(events.get(0).getRuleType().contains(FirewallChainTest.class.getName()));
    }
}
