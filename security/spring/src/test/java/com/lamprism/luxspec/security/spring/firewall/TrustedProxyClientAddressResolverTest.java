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

package com.lamprism.luxspec.security.spring.firewall;

import com.lamprism.luxspec.network.ip.IpAddress;
import com.lamprism.luxspec.network.ip.IpNetwork;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author RollW
 */
class TrustedProxyClientAddressResolverTest {
    @Test
    void resolvesTheFirstUntrustedAddressFromForwardedChain() {
        TrustedProxyClientAddressResolver resolver = resolver("10.0.0.0/8");
        MockHttpServletRequest request = request("10.0.0.10");
        request.addHeader("Forwarded", "for=203.0.113.42;proto=https, for=10.0.0.11");

        assertEquals(IpAddress.parse("203.0.113.42"), resolver.resolve(request));
    }

    @Test
    void resolvesTheFirstUntrustedAddressFromXForwardedFor() {
        TrustedProxyClientAddressResolver resolver = resolver("10.0.0.0/8");
        MockHttpServletRequest request = request("10.0.0.10");
        request.addHeader("X-Forwarded-For", "203.0.113.42, 10.0.0.11");

        assertEquals(IpAddress.parse("203.0.113.42"), resolver.resolve(request));
    }

    @Test
    void ignoresForwardingHeadersFromAnUntrustedPeer() {
        TrustedProxyClientAddressResolver resolver = resolver("10.0.0.0/8");
        MockHttpServletRequest request = request("198.51.100.10");
        request.addHeader("X-Forwarded-For", "203.0.113.42");

        assertEquals(IpAddress.parse("198.51.100.10"), resolver.resolve(request));
    }

    @Test
    void neverTrustsAnAmbiguousPeerOrAnAddressRangeInAForwardedHeader() {
        TrustedProxyClientAddressResolver resolver = resolver("10.0.0.0/8");
        MockHttpServletRequest ambiguous = request("010.0.0.1");
        ambiguous.addHeader("X-Forwarded-For", "203.0.113.42");
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(ambiguous));

        MockHttpServletRequest trusted = request("10.0.0.1");
        trusted.addHeader("X-Forwarded-For", "203.0.113.0/24");
        assertEquals(IpAddress.parse("10.0.0.1"), resolver.resolve(trusted));
    }

    @Test
    void rejectsWildcardOrRangeEntriesAsTrustedProxyNetworks() {
        assertThrows(IllegalArgumentException.class, () -> resolver("10.0.*.*"));
        assertThrows(IllegalArgumentException.class, () -> resolver("10.0.0.1-10.0.0.10"));
    }

    @Test
    void prefersForwardedWhenBothHeadersArePresent() {
        TrustedProxyClientAddressResolver resolver = resolver("10.0.0.0/8");
        MockHttpServletRequest request = request("10.0.0.10");
        request.addHeader("Forwarded", "for=203.0.113.42");
        request.addHeader("X-Forwarded-For", "198.51.100.42");

        assertEquals(IpAddress.parse("203.0.113.42"), resolver.resolve(request));
    }

    @Test
    void fallsBackToTheImmediatePeerWhenForwardingDataIsInvalid() {
        TrustedProxyClientAddressResolver resolver = resolver("10.0.0.0/8");
        MockHttpServletRequest request = request("10.0.0.10");
        request.addHeader("X-Forwarded-For", "unknown");

        assertEquals(IpAddress.parse("10.0.0.10"), resolver.resolve(request));
    }

    @Test
    void normalizesBracketedIpv6AndIgnoresItsPort() {
        TrustedProxyClientAddressResolver resolver = resolver("2001:db8:1::/48");
        MockHttpServletRequest request = request("2001:db8:1::10");
        request.addHeader("Forwarded", "for=\"[2001:db8::42]:8443\"");

        assertEquals(IpAddress.parse("2001:db8::42"), resolver.resolve(request));
    }

    @Test
    void rejectsAnEmptyTrustedProxyConfiguration() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TrustedProxyClientAddressResolver(List.of())
        );
    }

    private static TrustedProxyClientAddressResolver resolver(String network) {
        return new TrustedProxyClientAddressResolver(List.of(IpNetwork.parse(network)));
    }

    private static MockHttpServletRequest request(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}
