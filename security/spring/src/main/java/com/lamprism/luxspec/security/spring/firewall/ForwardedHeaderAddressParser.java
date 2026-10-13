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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class ForwardedHeaderAddressParser {
    private ForwardedHeaderAddressParser() {
    }

    static Optional<List<IPAddress>> parseForwarded(List<String> headerValues) {
        List<IPAddress> result = new ArrayList<>();
        for (String headerValue : headerValues) {
            Optional<List<String>> elements = splitOutsideQuotes(headerValue, ',');
            if (elements.isEmpty()) {
                return Optional.empty();
            }
            for (String element : elements.get()) {
                Optional<String> forValue = findForParameter(element);
                if (forValue.isEmpty()) {
                    return Optional.empty();
                }
                Optional<IPAddress> address = parseAddressToken(forValue.get());
                if (address.isEmpty()) {
                    return Optional.empty();
                }
                result.add(address.get());
            }
        }
        return result.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(result));
    }

    static Optional<List<IPAddress>> parseXForwardedFor(List<String> headerValues) {
        List<IPAddress> result = new ArrayList<>();
        for (String headerValue : headerValues) {
            Optional<List<String>> values = splitOutsideQuotes(headerValue, ',');
            if (values.isEmpty()) {
                return Optional.empty();
            }
            for (String value : values.get()) {
                Optional<IPAddress> address = parseAddressToken(value);
                if (address.isEmpty()) {
                    return Optional.empty();
                }
                result.add(address.get());
            }
        }
        return result.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(result));
    }

    private static Optional<String> findForParameter(String element) {
        Optional<List<String>> parameters = splitOutsideQuotes(element, ';');
        if (parameters.isEmpty()) {
            return Optional.empty();
        }
        Optional<String> forValue = Optional.empty();
        for (String parameter : parameters.get()) {
            int equalsIndex = parameter.indexOf('=');
            if (equalsIndex <= 0) {
                return Optional.empty();
            }
            String name = parameter.substring(0, equalsIndex).trim();
            if (!name.equalsIgnoreCase("for")) {
                continue;
            }
            if (forValue.isPresent()) {
                return Optional.empty();
            }
            Optional<String> value = unquote(parameter.substring(equalsIndex + 1).trim());
            if (value.isEmpty()) {
                return Optional.empty();
            }
            forValue = value;
        }
        return forValue;
    }

    private static Optional<List<String>> splitOutsideQuotes(String value, char separator) {
        List<String> result = new ArrayList<>();
        boolean quoted = false;
        boolean escaped = false;
        int start = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (quoted && current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                quoted = !quoted;
                continue;
            }
            if (!quoted && current == separator) {
                String part = value.substring(start, index).trim();
                if (part.isEmpty()) {
                    return Optional.empty();
                }
                result.add(part);
                start = index + 1;
            }
        }
        if (quoted || escaped) {
            return Optional.empty();
        }
        String lastPart = value.substring(start).trim();
        if (lastPart.isEmpty()) {
            return Optional.empty();
        }
        result.add(lastPart);
        return Optional.of(result);
    }

    private static Optional<String> unquote(String value) {
        if (value.isEmpty()) {
            return Optional.empty();
        }
        if (value.charAt(0) != '"') {
            return value.indexOf('"') >= 0 ? Optional.empty() : Optional.of(value);
        }
        if (value.length() < 2 || value.charAt(value.length() - 1) != '"') {
            return Optional.empty();
        }
        StringBuilder result = new StringBuilder(value.length() - 2);
        boolean escaped = false;
        for (int index = 1; index < value.length() - 1; index++) {
            char current = value.charAt(index);
            if (escaped) {
                result.append(current);
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                return Optional.empty();
            }
            result.append(current);
        }
        return escaped ? Optional.empty() : Optional.of(result.toString());
    }

    private static Optional<IPAddress> parseAddressToken(String token) {
        String value = token.trim();
        if (value.isEmpty()) {
            return Optional.empty();
        }
        if (value.charAt(0) == '[') {
            return parseBracketedAddress(value);
        }
        if (count(value, ':') == 1) {
            int portSeparator = value.lastIndexOf(':');
            if (isPort(value.substring(portSeparator + 1))) {
                value = value.substring(0, portSeparator);
            }
        }
        return parseAddress(value);
    }

    private static Optional<IPAddress> parseBracketedAddress(String value) {
        int closingBracket = value.indexOf(']');
        if (closingBracket <= 1) {
            return Optional.empty();
        }
        String address = value.substring(1, closingBracket);
        String suffix = value.substring(closingBracket + 1).trim();
        if (!suffix.isEmpty()) {
            if (suffix.charAt(0) != ':' || !isPort(suffix.substring(1))) {
                return Optional.empty();
            }
        }
        return parseAddress(address);
    }

    private static boolean isPort(String value) {
        if (value.isEmpty()) {
            return false;
        }
        int port = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current < '0' || current > '9') {
                return false;
            }
            int digit = current - '0';
            if (port > (65535 - digit) / 10) {
                return false;
            }
            port = port * 10 + digit;
        }
        return true;
    }

    private static int count(String value, char character) {
        int count = 0;
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) == character) {
                count++;
            }
        }
        return count;
    }

    private static Optional<IPAddress> parseAddress(String value) {
        if (value.isBlank()) {
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
