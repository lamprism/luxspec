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
import com.lamprism.luxspec.network.ip.IpAddressSet;
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

    private final List<IpAddressSet> trustedProxyNetworks;

    /**
     * Creates a resolver for trusted proxy addresses and networks.
     *
     * @param trustedProxyNetworks exact IP addresses or CIDR networks belonging to trusted proxies
     */
    public TrustedProxyClientAddressResolver(Collection<? extends IpAddressSet> trustedProxyNetworks) {
        this.trustedProxyNetworks = List.copyOf(Objects.requireNonNull(trustedProxyNetworks, "trustedProxyNetworks"));
        if (this.trustedProxyNetworks.isEmpty()) {
            throw new IllegalArgumentException("trustedProxyNetworks must not be empty");
        }
    }

    @Override
    public IpAddress resolve(HttpServletRequest request) {
        HttpServletRequest nonNullRequest = Objects.requireNonNull(request, "request");
        IpAddress remoteAddress = IpAddress.parse(Objects.requireNonNull(
                nonNullRequest.getRemoteAddr(),
                "request.remoteAddr"
        ));
        if (!isTrusted(remoteAddress)) {
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

    private Optional<IpAddress> resolveForwardedAddress(List<String> headerValues) {
        return ForwardedHeaderAddressParser.parseForwarded(headerValues)
                .flatMap(this::selectClientAddress);
    }

    private Optional<IpAddress> resolveXForwardedForAddress(List<String> headerValues) {
        return ForwardedHeaderAddressParser.parseXForwardedFor(headerValues)
                .flatMap(this::selectClientAddress);
    }

    private Optional<IpAddress> selectClientAddress(List<IpAddress> addressChain) {
        for (int index = addressChain.size() - 1; index >= 0; index--) {
            IpAddress address = addressChain.get(index);
            if (!isTrusted(address)) {
                return Optional.of(address);
            }
        }
        return Optional.empty();
    }

    private boolean isTrusted(IpAddress address) {
        for (IpAddressSet network : trustedProxyNetworks) {
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

}
