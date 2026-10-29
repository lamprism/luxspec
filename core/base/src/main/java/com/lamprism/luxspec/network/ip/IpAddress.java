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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

/**
 * An immutable IPv4 or IPv6 address, independent of DNS and socket endpoints.
 *
 * <p>Parsing accepts decimal IPv4 and IPv6 literals, including embedded IPv4. IPv4 octets with
 * leading zeros, zone identifiers, brackets, ports, and hostnames are not address literals for
 * this API. IPv4-mapped IPv6 addresses retain their IPv6 family.</p>
 *
 * @author RollW
 */
public final class IpAddress implements IpAddressSet, Comparable<IpAddress> {
    private final IpVersion version;
    private final byte[] bytes;

    private IpAddress(IpVersion version, byte[] bytes) {
        this.version = version;
        this.bytes = bytes.clone();
    }

    /**
     * Parses one IP address literal without network access.
     *
     * @param value the address literal
     * @return the parsed address
     */
    public static IpAddress parse(String value) {
        String text = Objects.requireNonNull(value, "value");
        if (text.isEmpty()) {
            throw IpAddressSyntax.invalid(text);
        }
        return text.indexOf(':') >= 0
                ? new IpAddress(IpVersion.IPV6, IpAddressSyntax.ipv6(text))
                : new IpAddress(IpVersion.IPV4, IpAddressSyntax.ipv4(text));
    }

    /**
     * Creates an address from four IPv4 or sixteen IPv6 network-order bytes.
     *
     * @param bytes the address bytes
     * @return the address
     */
    public static IpAddress of(byte[] bytes) {
        byte[] value = Objects.requireNonNull(bytes, "bytes");
        if (value.length == IpVersion.IPV4.byteCount()) {
            return new IpAddress(IpVersion.IPV4, value);
        }
        if (value.length == IpVersion.IPV6.byteCount()) {
            return new IpAddress(IpVersion.IPV6, value);
        }
        throw new IllegalArgumentException("An IP address requires four or sixteen bytes");
    }

    /**
     * Creates an address from an unsigned numeric value and an explicit address family.
     *
     * @param version the address family
     * @param number  the unsigned address value
     * @return the address
     */
    public static IpAddress of(IpVersion version, BigInteger number) {
        IpVersion family = Objects.requireNonNull(version, "version");
        BigInteger value = Objects.requireNonNull(number, "number");
        if (value.signum() < 0 || value.bitLength() > family.bitCount()) {
            throw new IllegalArgumentException("IP address number is outside its family");
        }
        byte[] source = value.toByteArray();
        byte[] result = new byte[family.byteCount()];
        int length = Math.min(source.length, result.length);
        System.arraycopy(source, source.length - length, result, result.length - length, length);
        return new IpAddress(family, result);
    }

    @Override
    public IpVersion version() {
        return version;
    }

    /**
     * Returns a defensive copy of the network-order address bytes.
     *
     * @return the address bytes
     */
    public byte[] bytes() {
        return bytes.clone();
    }

    /**
     * Returns the unsigned numeric address value.
     *
     * @return the address number
     */
    public BigInteger number() {
        return new BigInteger(1, bytes);
    }

    @Override
    public boolean contains(IpAddress address) {
        return equals(Objects.requireNonNull(address, "address"));
    }

    @Override
    public int compareTo(IpAddress other) {
        IpAddress nonNullOther = Objects.requireNonNull(other, "other");
        int family = version.compareTo(nonNullOther.version);
        if (family != 0) {
            return family;
        }
        for (int index = 0; index < bytes.length; index++) {
            int comparison = Integer.compare(bytes[index] & 0xff, nonNullOther.bytes[index] & 0xff);
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    /**
     * Returns IPv6 as eight lowercase hexadecimal groups, or IPv4 as dotted decimal.
     *
     * @return the expanded address text
     */
    public String toExpandedString() {
        if (version == IpVersion.IPV4) {
            return formatIpv4(bytes, 0);
        }
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < 8; index++) {
            if (index > 0) {
                result.append(':');
            }
            result.append(Integer.toHexString(group(index)));
        }
        return result.toString();
    }

    @Override
    public String toString() {
        if (version == IpVersion.IPV4) {
            return formatIpv4(bytes, 0);
        }
        if (isIpv4Mapped()) {
            return "::ffff:" + formatIpv4(bytes, 12);
        }
        int bestStart = -1;
        int bestLength = 0;
        for (int index = 0; index < 8; ) {
            if (group(index) != 0) {
                index++;
                continue;
            }
            int start = index;
            while (index < 8 && group(index) == 0) {
                index++;
            }
            if (index - start > bestLength && index - start >= 2) {
                bestStart = start;
                bestLength = index - start;
            }
        }
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < 8; ) {
            if (index == bestStart) {
                result.append("::");
                index += bestLength;
                continue;
            }
            if (!result.isEmpty() && result.charAt(result.length() - 1) != ':') {
                result.append(':');
            }
            result.append(Integer.toHexString(group(index++)));
        }
        return result.toString();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof IpAddress address && version == address.version && Arrays.equals(bytes, address.bytes);
    }

    @Override
    public int hashCode() {
        return 31 * version.hashCode() + Arrays.hashCode(bytes);
    }

    private boolean isIpv4Mapped() {
        for (int index = 0; index < 10; index++) {
            if (bytes[index] != 0) {
                return false;
            }
        }
        return (bytes[10] & 0xff) == 0xff && (bytes[11] & 0xff) == 0xff;
    }

    private int group(int index) {
        return (bytes[index * 2] & 0xff) << 8 | bytes[index * 2 + 1] & 0xff;
    }

    private static String formatIpv4(byte[] value, int offset) {
        return (value[offset] & 0xff) + "." + (value[offset + 1] & 0xff) + "."
                + (value[offset + 2] & 0xff) + "." + (value[offset + 3] & 0xff);
    }
}
