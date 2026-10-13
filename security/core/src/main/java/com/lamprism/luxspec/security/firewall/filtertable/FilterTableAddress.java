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

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;

import java.util.Objects;

final class FilterTableAddress {
    private FilterTableAddress() {
    }

    static String canonicalize(String value) {
        return parsePolicy(value).canonicalAddress();
    }

    static ParsedAddress parsePolicy(String value) {
        IPAddress address = parse(value, "policy address");
        if (address.isMultiple() && !address.isSinglePrefixBlock()) {
            throw invalidAddress(value, "ranges and wildcards are not supported");
        }
        Integer prefixLength = address.getPrefixLength();
        int bitCount = address.getBitCount();
        if (prefixLength == null || prefixLength == bitCount) {
            IPAddress exactAddress = address.withoutPrefixLength();
            return new ParsedAddress(exactAddress, bitCount, exactAddress.toNormalizedString());
        }
        IPAddress network = address.toPrefixBlock();
        return new ParsedAddress(network, prefixLength, network.toPrefixLengthString());
    }

    static IPAddress parseRequest(String value) {
        IPAddress address = parse(value, "request client address");
        if (address.isMultiple()) {
            throw invalidAddress(value, "request address must identify one IP address");
        }
        return address.withoutPrefixLength();
    }

    private static IPAddress parse(String value, String name) {
        String nonNullValue = Objects.requireNonNull(value, name);
        if (nonNullValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        IPAddressString parsed = new IPAddressString(nonNullValue);
        if (!parsed.isValid() || !parsed.isIPAddress()) {
            throw invalidAddress(value, "must be a valid IP address or CIDR network");
        }
        IPAddress address = parsed.getAddress();
        if (address == null) {
            throw invalidAddress(value, "must resolve to an IP address");
        }
        return address;
    }

    private static IllegalArgumentException invalidAddress(String value, String reason) {
        return new IllegalArgumentException("Invalid IP address '" + value + "': " + reason);
    }

    record ParsedAddress(IPAddress address, int specificity, String canonicalAddress) {
    }
}
