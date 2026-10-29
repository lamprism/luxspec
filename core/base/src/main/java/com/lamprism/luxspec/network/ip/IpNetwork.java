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
import java.util.Objects;

/**
 * An immutable CIDR address set with its host bits cleared.
 *
 * @author RollW
 */
public final class IpNetwork implements IpAddressSet {
    private final IpAddress first;
    private final int prefixLength;

    private IpNetwork(IpAddress first, int prefixLength) {
        this.first = first;
        this.prefixLength = prefixLength;
    }

    /**
     * Parses an IP literal with an optional CIDR prefix. Without a prefix the set contains one
     * exact address.
     *
     * @param value the address or CIDR
     * @return the canonical network
     */
    public static IpNetwork parse(String value) {
        String text = Objects.requireNonNull(value, "value");
        if (text.length() > 49) {
            throw IpAddressSyntax.invalid(text);
        }
        int slash = text.indexOf('/');
        if (slash < 0) {
            IpAddress address = IpAddress.parse(text);
            return of(address, address.version().bitCount());
        }
        if (slash != text.lastIndexOf('/')) {
            throw IpAddressSyntax.invalid(text);
        }
        IpAddress address = IpAddress.parse(text.substring(0, slash));
        int prefix = IpAddressSyntax.number(text.substring(slash + 1), 10, 3, address.version().bitCount(), false);
        return of(address, prefix);
    }

    /**
     * Creates a CIDR set from an address and prefix length.
     *
     * @param address      any address inside the network
     * @param prefixLength the number of significant high-order bits
     * @return the canonical network
     */
    public static IpNetwork of(IpAddress address, int prefixLength) {
        IpAddress nonNullAddress = Objects.requireNonNull(address, "address");
        if (prefixLength < 0 || prefixLength > nonNullAddress.version().bitCount()) {
            throw new IllegalArgumentException("Invalid IP network prefix length");
        }
        byte[] bytes = nonNullAddress.bytes();
        for (int index = 0; index < bytes.length; index++) {
            int remaining = prefixLength - index * Byte.SIZE;
            int mask = remaining >= Byte.SIZE ? 0xff : remaining <= 0 ? 0 : 0xff << (Byte.SIZE - remaining);
            bytes[index] = (byte) (bytes[index] & mask);
        }
        return new IpNetwork(IpAddress.of(bytes), prefixLength);
    }

    @Override
    public IpVersion version() {
        return first.version();
    }

    /**
     * Returns the canonical first address of the network.
     *
     * @return the first address
     */
    public IpAddress first() {
        return first;
    }

    /**
     * Returns the inclusive last address of the network.
     *
     * @return the last address
     */
    public IpAddress last() {
        byte[] bytes = first.bytes();
        for (int index = 0; index < bytes.length; index++) {
            int remaining = prefixLength - index * Byte.SIZE;
            int mask = remaining >= Byte.SIZE ? 0 : remaining <= 0 ? 0xff : 0xff >>> remaining;
            bytes[index] = (byte) (bytes[index] | mask);
        }
        return IpAddress.of(bytes);
    }

    /**
     * Returns the prefix length in bits.
     *
     * @return the prefix length
     */
    public int prefixLength() {
        return prefixLength;
    }

    /**
     * Returns the total number of addresses, including every boundary address.
     *
     * @return the address count
     */
    public BigInteger size() {
        return BigInteger.ONE.shiftLeft(version().bitCount() - prefixLength);
    }

    @Override
    public boolean contains(IpAddress address) {
        IpAddress candidate = Objects.requireNonNull(address, "address");
        if (candidate.version() != version()) {
            return false;
        }
        return of(candidate, prefixLength).first.equals(first);
    }

    /**
     * Tests whether this network contains the complete other network.
     *
     * @param other the candidate network
     * @return whether all of its addresses are contained
     */
    public boolean contains(IpNetwork other) {
        IpNetwork candidate = Objects.requireNonNull(other, "other");
        return version() == candidate.version()
                && prefixLength <= candidate.prefixLength
                && contains(candidate.first);
    }

    /**
     * Tests whether the two CIDR networks share any address.
     *
     * @param other the other network
     * @return whether the networks overlap
     */
    public boolean overlaps(IpNetwork other) {
        IpNetwork candidate = Objects.requireNonNull(other, "other");
        return contains(candidate.first) || candidate.contains(first);
    }

    @Override
    public String toString() {
        return first + "/" + prefixLength;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof IpNetwork network
                && prefixLength == network.prefixLength
                && first.equals(network.first);
    }

    @Override
    public int hashCode() {
        return 31 * first.hashCode() + prefixLength;
    }
}
