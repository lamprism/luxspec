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

package com.lamprism.luxspec.security.firewall.filtertable;

import com.lamprism.luxspec.network.ip.IpAddress;
import com.lamprism.luxspec.network.ip.IpNetwork;
import com.lamprism.luxspec.security.SecurityErrorCode;
import com.lamprism.luxspec.security.authentication.UserSubject;
import com.lamprism.luxspec.security.firewall.FirewallDecision;
import com.lamprism.luxspec.security.firewall.FirewallRequest;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author RollW
 */
class FilterTableTest {
    private static final Instant NOW = Instant.parse("2026-09-20T00:00:00Z");

    private static final class TestRequest implements FirewallRequest {
        private final IpAddress clientAddress;
        private final UserSubject subject;

        private TestRequest(String clientAddress, UserSubject subject) {
            this.clientAddress = IpAddress.parse(clientAddress);
            this.subject = subject;
        }

        @Override
        public IpAddress getClientAddress() {
            return clientAddress;
        }

        @Override
        public UserSubject getSubject() {
            return subject;
        }
    }

    @Test
    void atomicallyPublishesInMemoryChangesAndKeepsSnapshotsImmutable() {
        InMemoryFilterTable table = new InMemoryFilterTable();
        FilterEntry entry = FilterEntry.deny(FilterTarget.clientAddress(IpNetwork.parse("192.0.2.1")));

        FilterTableSnapshot before = table.snapshot();
        table.put(entry);

        assertTrue(before.getEntries().isEmpty());
        assertEquals(entry, table.get(entry.target()));
        assertThrows(UnsupportedOperationException.class, () -> table.getEntries().add(entry));

        assertTrue(table.remove(entry.target()));
        assertNull(table.get(entry.target()));
        assertFalse(table.remove(entry.target()));
    }

    @Test
    void usesLongestActiveAddressPrefixAndFallsBackAfterExpiration() {
        FilterEntry broadAllow = FilterEntry.allow(FilterTarget.clientAddress(IpNetwork.parse("10.0.0.0/8")));
        FilterEntry expiredNarrowDeny = new FilterEntry(
                FilterTarget.clientAddress(IpNetwork.parse("10.1.0.0/16")),
                FilterMode.DENY,
                NOW,
                "temporary block"
        );
        FilterTableSnapshot snapshot = new FilterTableSnapshot(List.of(broadAllow, expiredNarrowDeny));

        FilterEntry match = snapshot.findMatch(new TestRequest("10.1.2.3", null), NOW);

        assertEquals(broadAllow, match);
    }

    @Test
    void givesActiveSubjectEntryPrecedenceOverAddressEntry() {
        UserSubject subject = new UserSubject(7L);
        FilterEntry addressDeny = FilterEntry.deny(FilterTarget.clientAddress(IpNetwork.parse("198.51.100.0/24")));
        FilterEntry subjectAllow = FilterEntry.allow(FilterTarget.subject(subject));
        FilterTableFirewall firewall = new FilterTableFirewall(
                new InMemoryFilterTable(List.of(addressDeny, subjectAllow)),
                0,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        FirewallDecision decision = firewall.verifyRequest(new TestRequest("198.51.100.42", subject));

        assertEquals(FirewallDecision.Case.ALLOW, decision.getCase());
    }

    @Test
    void adaptsNoMatchAllowAndDenyToRegistryDecisions() {
        InMemoryFilterTable table = new InMemoryFilterTable();
        FilterTableFirewall firewall = new FilterTableFirewall(
                table,
                0,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        TestRequest request = new TestRequest("203.0.113.10", null);

        assertEquals(FirewallDecision.Case.NEUTRAL, firewall.verifyRequest(request).getCase());
        table.put(FilterEntry.allow(FilterTarget.clientAddress(IpNetwork.parse("203.0.113.10"))));
        assertEquals(FirewallDecision.Case.ALLOW, firewall.verifyRequest(request).getCase());
        table.put(FilterEntry.deny(FilterTarget.clientAddress(IpNetwork.parse("203.0.113.10"))));
        FirewallDecision denied = firewall.verifyRequest(request);
        assertEquals(FirewallDecision.Case.DENY, denied.getCase());
        assertEquals(SecurityErrorCode.FIREWALL_CLIENT_ADDRESS_DENIED, denied.getReasonCode());
    }

    @Test
    void rejectsInvalidOrDuplicateTargets() {
        assertThrows(
                IllegalArgumentException.class,
                () -> FilterTarget.clientAddress(IpNetwork.parse("192.0.2.1-192.0.2.2"))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> FilterTarget.clientAddress(IpNetwork.parse("192.0.2.*"))
        );

        FilterTarget target = FilterTarget.clientAddress(IpNetwork.parse("192.0.2.1"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new FilterTableSnapshot(List.of(
                        FilterEntry.allow(target),
                        FilterEntry.deny(target)
                ))
        );
    }
}
