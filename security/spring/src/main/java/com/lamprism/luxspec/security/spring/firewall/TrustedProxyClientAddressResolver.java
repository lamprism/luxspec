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

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves a client address from forwarding headers emitted by explicitly trusted proxies.
 *
 * <p>The immediate Servlet peer must match one of the configured proxy addresses or CIDR
 * networks before either forwarding header is used. RFC 7239 {@code Forwarded} is preferred when
 * it is present. {@code X-Forwarded-For} is used only when {@code Forwarded} is absent. The
 * selected address is the first address outside the trusted proxy networks when the chain is
 * inspected from the nearest hop toward the client.</p>
 *
 * <p>The trusted proxies must remove or correctly append client-supplied forwarding headers. No
 * application-side resolver can distinguish a genuine client hop from a spoofed hop if a trusted
 * proxy forwards an untrusted header unchanged.</p>
 *
 * <p>Malformed or unusable forwarding data falls back to the immediate peer address. Applications
 * that require a different header policy or failure behavior can provide their own
 * {@link ServletClientAddressResolver} implementation.</p>
 *
 * @author RollW
 */
public final class TrustedProxyClientAddressResolver implements ServletClientAddressResolver {
    private static final String FORWARDED_HEADER = "Forwarded";
    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private final List<IPAddress> trustedProxyNetworks;

    /**
     * Creates a resolver for trusted proxy addresses and networks.
     *
     * @param trustedProxyNetworks exact IP addresses or CIDR networks belonging to trusted proxies
     */
    public TrustedProxyClientAddressResolver(Collection<String> trustedProxyNetworks) {
        this.trustedProxyNetworks = parseTrustedProxyNetworks(trustedProxyNetworks);
    }

    @Override
    public String resolve(HttpServletRequest request) {
        HttpServletRequest nonNullRequest = Objects.requireNonNull(request, "request");
        String remoteAddress = Objects.requireNonNull(
                nonNullRequest.getRemoteAddr(),
                "request.remoteAddr"
        );
        Optional<IPAddress> parsedRemoteAddress = parseAddress(remoteAddress);
        if (parsedRemoteAddress.isEmpty() || !isTrusted(parsedRemoteAddress.get())) {
            return remoteAddress;
        }

        List<String> forwardedValues = headerValues(nonNullRequest, FORWARDED_HEADER);
        if (!forwardedValues.isEmpty()) {
            return resolveForwardedAddress(forwardedValues).orElse(remoteAddress);
        }

        List<String> xForwardedForValues = headerValues(nonNullRequest, X_FORWARDED_FOR_HEADER);
        if (!xForwardedForValues.isEmpty()) {
            return resolveXForwardedForAddress(xForwardedForValues).orElse(remoteAddress);
        }
        return remoteAddress;
    }

    private Optional<String> resolveForwardedAddress(List<String> headerValues) {
        return ForwardedHeaderAddressParser.parseForwarded(headerValues)
                .flatMap(this::selectClientAddress);
    }

    private Optional<String> resolveXForwardedForAddress(List<String> headerValues) {
        return ForwardedHeaderAddressParser.parseXForwardedFor(headerValues)
                .flatMap(this::selectClientAddress);
    }

    private Optional<String> selectClientAddress(List<IPAddress> addressChain) {
        for (int index = addressChain.size() - 1; index >= 0; index--) {
            IPAddress address = addressChain.get(index);
            if (!isTrusted(address)) {
                return Optional.of(address.withoutPrefixLength().toNormalizedString());
            }
        }
        return Optional.empty();
    }

    private boolean isTrusted(IPAddress address) {
        for (IPAddress network : trustedProxyNetworks) {
            if (network.getIPVersion() != address.getIPVersion()) {
                continue;
            }
            if (network.contains(address)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> headerValues(HttpServletRequest request, String headerName) {
        Enumeration<String> values = request.getHeaders(headerName);
        if (values == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        while (values.hasMoreElements()) {
            String value = values.nextElement();
            if (value != null && !value.isBlank()) {
                result.add(value);
            }
        }
        return List.copyOf(result);
    }

    private static List<IPAddress> parseTrustedProxyNetworks(Collection<String> values) {
        Collection<String> nonNullValues = Objects.requireNonNull(values, "trustedProxyNetworks");
        if (nonNullValues.isEmpty()) {
            throw new IllegalArgumentException("trustedProxyNetworks must not be empty");
        }
        List<IPAddress> result = new ArrayList<>(nonNullValues.size());
        for (String value : nonNullValues) {
            result.add(parseNetwork(value));
        }
        return List.copyOf(result);
    }

    private static IPAddress parseNetwork(String value) {
        String nonNullValue = Objects.requireNonNull(value, "trustedProxyNetwork").trim();
        if (nonNullValue.isEmpty()) {
            throw new IllegalArgumentException("trustedProxyNetwork must not be blank");
        }
        IPAddressString parsed = new IPAddressString(nonNullValue);
        if (!parsed.isValid() || !parsed.isIPAddress()) {
            throw new IllegalArgumentException(
                    "Invalid trusted proxy address or network '" + nonNullValue + "'"
            );
        }
        IPAddress address = parsed.getAddress();
        if (address == null) {
            throw new IllegalArgumentException(
                    "Invalid trusted proxy address or network '" + nonNullValue + "'"
            );
        }
        if (address.isMultiple() && !address.isSinglePrefixBlock()) {
            throw new IllegalArgumentException(
                    "Trusted proxy value must be an IP address or CIDR network: '"
                            + nonNullValue + "'"
            );
        }
        if (address.isMultiple()) {
            return address.toPrefixBlock();
        }
        return address.withoutPrefixLength();
    }

    private static Optional<IPAddress> parseAddress(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        IPAddressString parsed = new IPAddressString(value);
        if (!parsed.isValid() || !parsed.isIPAddress()) {
            return Optional.empty();
        }
        IPAddress address = parsed.getAddress();
        if (address == null || address.isMultiple()) {
            return Optional.empty();
        }
        return Optional.of(address.withoutPrefixLength());
    }
}
