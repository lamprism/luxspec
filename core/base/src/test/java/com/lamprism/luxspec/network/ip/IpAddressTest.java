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

package com.lamprism.luxspec.network.ip;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author RollW
 */
class IpAddressTest {
    @Test
    void parsesAndFormatsIpv4AndIpv6Literals() {
        assertEquals("192.0.2.1", IpAddress.parse("192.0.2.1").toString());
        assertEquals("::", IpAddress.parse("0:0:0:0:0:0:0:0").toString());
        assertEquals("::1", IpAddress.parse("0:0:0:0:0:0:0:1").toString());
        assertEquals("2001:db8::1:0:0:1", IpAddress.parse("2001:DB8:0:0:1:0:0:1").toString());
        assertEquals("2001:db8:0:1:1:1:1:1", IpAddress.parse("2001:db8:0:1:1:1:1:1").toString());
        assertEquals("::ffff:192.0.2.1", IpAddress.parse("::ffff:c000:201").toString());
        assertEquals(IpVersion.IPV6, IpAddress.parse("::ffff:192.0.2.1").version());
        assertFalse(IpAddress.parse("::ffff:192.0.2.1").equals(IpAddress.parse("192.0.2.1")));
    }

    @Test
    void rejectsAmbiguousOrInvalidLiterals() {
        List<String> invalid = List.of(
                "", "localhost", "192.0.2", "192.0.2.256", "192.0.2.01", " 192.0.2.1",
                "192.0.2.1:80", "[::1]", "fe80::1%eth0", "1::2::3", ":::1",
                "1:2:3:4:5:6:7:8:9", "1:2:3:4:5:6:7:192.0.2.1", "::ffff:192.0.2.999"
        );
        for (String value : invalid) {
            assertThrows(IllegalArgumentException.class, () -> IpAddress.parse(value), value);
        }
    }

    @Test
    void modelsCidrBoundariesAndAddressFamilies() {
        IpNetwork network = IpNetwork.parse("192.0.2.123/25");
        assertEquals("192.0.2.0/25", network.toString());
        assertEquals(IpAddress.parse("192.0.2.0"), network.first());
        assertEquals(IpAddress.parse("192.0.2.127"), network.last());
        assertEquals(BigInteger.valueOf(128), network.size());
        assertTrue(network.contains(IpAddress.parse("192.0.2.127")));
        assertFalse(network.contains(IpAddress.parse("192.0.2.128")));
        assertFalse(network.contains(IpAddress.parse("::ffff:192.0.2.1")));
        assertTrue(network.contains(IpNetwork.parse("192.0.2.64/26")));
        assertFalse(network.overlaps(IpNetwork.parse("192.0.2.128/25")));
        assertEquals(BigInteger.ONE.shiftLeft(128), IpNetwork.parse("::/0").size());
        assertEquals(IpAddress.parse("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"), IpNetwork.parse("::/0").last());
        assertThrows(IllegalArgumentException.class, () -> IpNetwork.parse("192.0.2.1/33"));
        assertThrows(IllegalArgumentException.class, () -> IpNetwork.parse("::1/-1"));
    }

    @Test
    void supportsInclusiveRangesAndIndependentSegmentPatterns() {
        IpRange first = IpRange.parse("192.0.2.10-192.0.2.20");
        IpRange second = IpRange.parse("192.0.2.18-192.0.2.30");
        assertEquals(BigInteger.valueOf(11), first.size());
        assertEquals(IpRange.parse("192.0.2.18-192.0.2.20"), first.intersection(second).orElseThrow());
        assertTrue(first.intersection(IpRange.parse("::1-::2")).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> IpRange.parse("192.0.2.20-192.0.2.10"));

        IpPattern pattern = IpPattern.parse("10.1-2.*.7");
        assertEquals(BigInteger.valueOf(512), pattern.size());
        assertTrue(pattern.contains(IpAddress.parse("10.2.255.7")));
        assertFalse(pattern.contains(IpAddress.parse("10.3.0.7")));
        assertFalse(pattern.contains(IpAddress.parse("10.2.0.8")));
        IpPattern ipv6 = IpPattern.parse("2001:db8:0-ff::*");
        assertTrue(ipv6.contains(IpAddress.parse("2001:db8:ff::a")));
        assertFalse(ipv6.contains(IpAddress.parse("2001:db8:100::a")));
        assertEquals(ipv6, IpPattern.parse(ipv6.toString()));
    }

    @Test
    void convertsNumbersAndRangesToExactMinimalNetworks() {
        assertEquals(IpAddress.parse("255.255.255.255"),
                IpAddress.of(IpVersion.IPV4, new BigInteger("4294967295")));
        assertEquals(IpAddress.parse("::"), IpAddress.of(IpVersion.IPV6, BigInteger.ZERO));
        assertThrows(IllegalArgumentException.class, () -> IpAddress.of(IpVersion.IPV4, BigInteger.valueOf(-1)));
        assertThrows(IllegalArgumentException.class, () -> IpAddress.of(IpVersion.IPV4, BigInteger.ONE.shiftLeft(32)));
        assertEquals(List.of(IpNetwork.parse("0.0.0.0/0")),
                IpRange.parse("0.0.0.0-255.255.255.255").toNetworks());
        assertEquals(List.of(IpNetwork.parse("::/0")),
                IpRange.parse("::-ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff").toNetworks());
        assertEquals(List.of(
                IpNetwork.parse("192.0.2.5/32"), IpNetwork.parse("192.0.2.6/31"),
                IpNetwork.parse("192.0.2.8/30"), IpNetwork.parse("192.0.2.12/31"),
                IpNetwork.parse("192.0.2.14/32")
        ), IpRange.parse("192.0.2.5-192.0.2.14").toNetworks());
        assertEquals(List.of(IpNetwork.parse("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128")),
                IpRange.parse("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff-ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")
                        .toNetworks());
    }

    @Test
    void rangeNetworksHaveNoGapsOverlapsOrMergeableNeighbors() {
        Random random = new Random(820453L);
        for (IpVersion version : IpVersion.values()) {
            for (int iteration = 0; iteration < 500; iteration++) {
                byte[] firstBytes = new byte[version.byteCount()];
                byte[] lastBytes = new byte[version.byteCount()];
                random.nextBytes(firstBytes);
                random.nextBytes(lastBytes);
                IpAddress first = IpAddress.of(firstBytes);
                IpAddress last = IpAddress.of(lastBytes);
                IpRange range = first.compareTo(last) <= 0 ? new IpRange(first, last) : new IpRange(last, first);
                List<IpNetwork> networks = range.toNetworks();
                BigInteger next = range.first().number();
                IpNetwork previous = null;
                for (IpNetwork network : networks) {
                    assertEquals(next, network.first().number());
                    if (previous != null && previous.prefixLength() == network.prefixLength()) {
                        IpNetwork parent = IpNetwork.of(previous.first(), previous.prefixLength() - 1);
                        assertFalse(parent.contains(network), "Adjacent networks must not be mergeable");
                    }
                    next = network.last().number().add(BigInteger.ONE);
                    previous = network;
                }
                assertEquals(range.last().number().add(BigInteger.ONE), next);
            }
        }
    }

    @Test
    void agreesWithTheExistingIpLibraryForGeneratedAddressesAndNetworks() {
        Random random = new Random(742345L);
        for (IpVersion version : IpVersion.values()) {
            for (int iteration = 0; iteration < 1500; iteration++) {
                byte[] networkBytes = new byte[version.byteCount()];
                byte[] candidateBytes = new byte[version.byteCount()];
                random.nextBytes(networkBytes);
                random.nextBytes(candidateBytes);
                IpAddress networkAddress = IpAddress.of(networkBytes);
                IpAddress candidate = IpAddress.of(candidateBytes);
                int prefix = random.nextInt(version.bitCount() + 1);
                IpNetwork network = IpNetwork.of(networkAddress, prefix);
                IPAddress referenceNetwork = new IPAddressString(
                        networkAddress.toExpandedString() + "/" + prefix
                ).getAddress().toPrefixBlock();
                IPAddress referenceCandidate = new IPAddressString(candidate.toExpandedString()).getAddress();

                assertEquals(networkAddress, IpAddress.parse(networkAddress.toString()));
                assertEquals(referenceNetwork.contains(referenceCandidate), network.contains(candidate));
                assertArrayEquals(referenceNetwork.getLower().getBytes(), network.first().bytes());
                assertArrayEquals(referenceNetwork.getUpper().getBytes(), network.last().bytes());
            }
        }
    }
}
