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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author RollW
 */
final class IpAddressSyntax {
    private IpAddressSyntax() {
    }

    static byte[] ipv4(String value) {
        if (value.length() > 15) {
            throw invalid(value);
        }
        List<String> segments = split(value, '.');
        if (segments.size() != 4) {
            throw invalid(value);
        }
        byte[] result = new byte[4];
        for (int index = 0; index < result.length; index++) {
            result[index] = (byte) number(segments.get(index), 10, 3, 255, true);
        }
        return result;
    }

    static byte[] ipv6(String value) {
        if (value.length() > 45 || value.indexOf('%') >= 0) {
            throw invalid(value);
        }
        String expandedValue = value;
        if (value.indexOf('.') >= 0) {
            int colon = value.lastIndexOf(':');
            if (colon < 0) {
                throw invalid(value);
            }
            byte[] ipv4 = ipv4(value.substring(colon + 1));
            int high = (ipv4[0] & 0xff) << 8 | ipv4[1] & 0xff;
            int low = (ipv4[2] & 0xff) << 8 | ipv4[3] & 0xff;
            expandedValue = value.substring(0, colon + 1)
                    + Integer.toHexString(high) + ':' + Integer.toHexString(low);
        }
        List<String> segments = expandIpv6(expandedValue);
        byte[] result = new byte[16];
        for (int index = 0; index < segments.size(); index++) {
            int segment = number(segments.get(index), 16, 4, 0xffff, false);
            result[index * 2] = (byte) (segment >>> 8);
            result[index * 2 + 1] = (byte) segment;
        }
        return result;
    }

    static List<String> expandIpv6(String value) {
        int compression = value.indexOf("::");
        if (compression < 0) {
            List<String> segments = split(value, ':');
            if (segments.size() != 8) {
                throw invalid(value);
            }
            return segments;
        }
        if (value.indexOf("::", compression + 2) >= 0) {
            throw invalid(value);
        }
        String left = value.substring(0, compression);
        String right = value.substring(compression + 2);
        List<String> leading = left.isEmpty() ? List.of() : split(left, ':');
        List<String> trailing = right.isEmpty() ? List.of() : split(right, ':');
        int present = leading.size() + trailing.size();
        if (present >= 8) {
            throw invalid(value);
        }
        List<String> result = new ArrayList<>(8);
        result.addAll(leading);
        for (int index = present; index < 8; index++) {
            result.add("0");
        }
        result.addAll(trailing);
        return result;
    }

    static List<String> split(String value, char separator) {
        List<String> result = new ArrayList<>();
        int start = 0;
        for (int index = 0; index <= value.length(); index++) {
            if (index != value.length() && value.charAt(index) != separator) {
                continue;
            }
            if (index == start) {
                throw invalid(value);
            }
            result.add(value.substring(start, index));
            start = index + 1;
        }
        return result;
    }

    static int number(String value, int radix, int maxDigits, int maximum, boolean noLeadingZeros) {
        if (value.isEmpty() || value.length() > maxDigits
                || noLeadingZeros && value.length() > 1 && value.charAt(0) == '0') {
            throw invalid(value);
        }
        int result = 0;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            int digit;
            if (character >= '0' && character <= '9') {
                digit = character - '0';
            } else if (character >= 'a' && character <= 'f') {
                digit = character - 'a' + 10;
            } else if (character >= 'A' && character <= 'F') {
                digit = character - 'A' + 10;
            } else {
                throw invalid(value);
            }
            if (digit >= radix) {
                throw invalid(value);
            }
            result = result * radix + digit;
            if (result > maximum) {
                throw invalid(value);
            }
        }
        return result;
    }

    static IllegalArgumentException invalid(String value) {
        return new IllegalArgumentException("Invalid IP address syntax: " + value);
    }
}
