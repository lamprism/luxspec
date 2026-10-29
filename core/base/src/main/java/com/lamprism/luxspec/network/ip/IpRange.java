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
import java.util.Optional;

/**
 * An inclusive contiguous range of addresses in one IP family.
 *
 * @author RollW
 */
public final class IpRange implements IpAddressSet {
    private final IpAddress first;
    private final IpAddress last;

    /**
     * Creates an inclusive address range.
     *
     * @param first the first address
     * @param last  the last address
     */
    public IpRange(IpAddress first, IpAddress last) {
        this.first = Objects.requireNonNull(first, "first");
        this.last = Objects.requireNonNull(last, "last");
        if (first.version() != last.version() || first.compareTo(last) > 0) {
            throw new IllegalArgumentException("IP range bounds must use one family and be ordered");
        }
    }

    /**
     * Parses two IP address literals separated by a hyphen.
     *
     * @param value the inclusive range
     * @return the parsed range
     */
    public static IpRange parse(String value) {
        String text = Objects.requireNonNull(value, "value");
        if (text.length() > 91) {
            throw IpAddressSyntax.invalid(text);
        }
        int separator = text.indexOf('-');
        if (separator < 1 || separator != text.lastIndexOf('-') || separator == text.length() - 1) {
            throw IpAddressSyntax.invalid(text);
        }
        return new IpRange(IpAddress.parse(text.substring(0, separator)), IpAddress.parse(text.substring(separator + 1)));
    }

    @Override
    public IpVersion version() {
        return first.version();
    }

    /**
     * Returns the first inclusive address.
     *
     * @return the first address
     */
    public IpAddress first() {
        return first;
    }

    /**
     * Returns the last inclusive address.
     *
     * @return the last address
     */
    public IpAddress last() {
        return last;
    }

    /**
     * Returns the inclusive number of addresses.
     *
     * @return the address count
     */
    public BigInteger size() {
        return last.number().subtract(first.number()).add(BigInteger.ONE);
    }

    /**
     * Returns the smallest ordered list of CIDR networks covering exactly this range.
     *
     * @return non-overlapping networks in ascending address order
     */
    public List<IpNetwork> toNetworks() {
        List<IpNetwork> result = new ArrayList<>();
        BigInteger current = first.number();
        BigInteger end = last.number();
        int bits = version().bitCount();
        while (current.compareTo(end) <= 0) {
            int alignment = current.signum() == 0 ? bits : current.getLowestSetBit();
            int available = end.subtract(current).add(BigInteger.ONE).bitLength() - 1;
            int hostBits = Math.min(alignment, available);
            result.add(IpNetwork.of(IpAddress.of(version(), current), bits - hostBits));
            current = current.add(BigInteger.ONE.shiftLeft(hostBits));
        }
        return List.copyOf(result);
    }

    @Override
    public boolean contains(IpAddress address) {
        IpAddress candidate = Objects.requireNonNull(address, "address");
        return candidate.version() == version()
                && first.compareTo(candidate) <= 0
                && last.compareTo(candidate) >= 0;
    }

    /**
     * Returns the shared inclusive range, if any.
     *
     * @param other the other range
     * @return the intersection, or empty when the ranges are disjoint
     */
    public Optional<IpRange> intersection(IpRange other) {
        IpRange candidate = Objects.requireNonNull(other, "other");
        if (version() != candidate.version()) {
            return Optional.empty();
        }
        IpAddress start = first.compareTo(candidate.first) >= 0 ? first : candidate.first;
        IpAddress end = last.compareTo(candidate.last) <= 0 ? last : candidate.last;
        return start.compareTo(end) > 0 ? Optional.empty() : Optional.of(new IpRange(start, end));
    }

    @Override
    public String toString() {
        return first + "-" + last;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof IpRange range && first.equals(range.first) && last.equals(range.last);
    }

    @Override
    public int hashCode() {
        return 31 * first.hashCode() + last.hashCode();
    }
}
