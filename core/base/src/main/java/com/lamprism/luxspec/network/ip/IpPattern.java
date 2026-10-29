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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An address set with an exact value, wildcard, or inclusive range in each address segment.
 *
 * <p>IPv4 segments use decimal octets; IPv6 segments use hexadecimal groups and may use the
 * normal zero-compression syntax. A segment is an exact value, {@code *}, or {@code first-last}.
 * Embedded IPv4 is accepted as a literal suffix. Segment patterns are independent, so this set
 * is not necessarily a contiguous {@link IpRange}.</p>
 *
 * @author RollW
 */
public final class IpPattern implements IpAddressSet {
    private final IpVersion version;
    private final List<Segment> segments;

    private IpPattern(IpVersion version, List<Segment> segments) {
        this.version = version;
        this.segments = List.copyOf(segments);
    }

    /**
     * Parses an IPv4 or IPv6 segment pattern without resolving hostnames.
     *
     * @param value the segment pattern
     * @return the parsed pattern
     */
    public static IpPattern parse(String value) {
        String text = Objects.requireNonNull(value, "value");
        if (text.isEmpty() || text.length() > 128 || text.indexOf('%') >= 0) {
            throw IpAddressSyntax.invalid(text);
        }
        IpVersion version = text.indexOf(':') >= 0 ? IpVersion.IPV6 : IpVersion.IPV4;
        String normalized = version == IpVersion.IPV6 ? replaceIpv4Suffix(text) : text;
        List<String> source = version == IpVersion.IPV6
                ? IpAddressSyntax.expandIpv6(normalized)
                : IpAddressSyntax.split(normalized, '.');
        int expected = version == IpVersion.IPV6 ? 8 : 4;
        if (source.size() != expected) {
            throw IpAddressSyntax.invalid(text);
        }
        List<Segment> segments = new ArrayList<>(expected);
        for (String segment : source) {
            segments.add(parseSegment(segment, version));
        }
        return new IpPattern(version, segments);
    }

    @Override
    public IpVersion version() {
        return version;
    }

    @Override
    public boolean contains(IpAddress address) {
        IpAddress candidate = Objects.requireNonNull(address, "address");
        if (candidate.version() != version) {
            return false;
        }
        byte[] bytes = candidate.bytes();
        for (int index = 0; index < segments.size(); index++) {
            int value = version == IpVersion.IPV4
                    ? bytes[index] & 0xff
                    : (bytes[index * 2] & 0xff) << 8 | bytes[index * 2 + 1] & 0xff;
            Segment segment = segments.get(index);
            if (value < segment.first || value > segment.last) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the number of addresses represented by the independent segments.
     *
     * @return the address count
     */
    public BigInteger size() {
        BigInteger result = BigInteger.ONE;
        for (Segment segment : segments) {
            result = result.multiply(BigInteger.valueOf((long) segment.last - segment.first + 1));
        }
        return result;
    }

    @Override
    public String toString() {
        int radix = version == IpVersion.IPV4 ? 10 : 16;
        int maximum = version == IpVersion.IPV4 ? 0xff : 0xffff;
        char separator = version == IpVersion.IPV4 ? '.' : ':';
        StringBuilder result = new StringBuilder();
        for (Segment segment : segments) {
            if (!result.isEmpty()) {
                result.append(separator);
            }
            if (segment.first == 0 && segment.last == maximum) {
                result.append('*');
            } else {
                result.append(Integer.toString(segment.first, radix));
                if (segment.first != segment.last) {
                    result.append('-').append(Integer.toString(segment.last, radix));
                }
            }
        }
        return result.toString();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof IpPattern pattern && version == pattern.version && segments.equals(pattern.segments);
    }

    @Override
    public int hashCode() {
        return 31 * version.hashCode() + segments.hashCode();
    }

    private static Segment parseSegment(String value, IpVersion version) {
        int maximum = version == IpVersion.IPV4 ? 0xff : 0xffff;
        if (value.equals("*")) {
            return new Segment(0, maximum);
        }
        int separator = value.indexOf('-');
        if (separator < 0) {
            int exact = segmentNumber(value, version);
            return new Segment(exact, exact);
        }
        if (separator != value.lastIndexOf('-')) {
            throw IpAddressSyntax.invalid(value);
        }
        int first = segmentNumber(value.substring(0, separator), version);
        int last = segmentNumber(value.substring(separator + 1), version);
        if (first > last) {
            throw IpAddressSyntax.invalid(value);
        }
        return new Segment(first, last);
    }

    private static int segmentNumber(String value, IpVersion version) {
        if (version == IpVersion.IPV4) {
            return IpAddressSyntax.number(value, 10, 3, 255, true);
        }
        return IpAddressSyntax.number(value, 16, 4, 0xffff, false);
    }

    private static String replaceIpv4Suffix(String value) {
        if (value.indexOf('.') < 0) {
            return value;
        }
        int colon = value.lastIndexOf(':');
        if (colon < 0) {
            throw IpAddressSyntax.invalid(value);
        }
        byte[] ipv4 = IpAddressSyntax.ipv4(value.substring(colon + 1));
        int first = (ipv4[0] & 0xff) << 8 | ipv4[1] & 0xff;
        int last = (ipv4[2] & 0xff) << 8 | ipv4[3] & 0xff;
        return value.substring(0, colon + 1) + Integer.toHexString(first) + ':' + Integer.toHexString(last);
    }

    private static final class Segment {
        private final int first;
        private final int last;

        private Segment(int first, int last) {
            this.first = first;
            this.last = last;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Segment segment && first == segment.first && last == segment.last;
        }

        @Override
        public int hashCode() {
            return 31 * first + last;
        }
    }
}
