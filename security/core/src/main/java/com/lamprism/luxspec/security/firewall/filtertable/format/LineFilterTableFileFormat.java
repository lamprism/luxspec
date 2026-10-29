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

package com.lamprism.luxspec.security.firewall.filtertable.format;

import com.lamprism.luxspec.network.ip.IpNetwork;
import com.lamprism.luxspec.security.authentication.SimpleSubject;
import com.lamprism.luxspec.security.firewall.filtertable.FilterEntry;
import com.lamprism.luxspec.security.firewall.filtertable.FilterMode;
import com.lamprism.luxspec.security.firewall.filtertable.FilterTableSnapshot;
import com.lamprism.luxspec.security.firewall.filtertable.FilterTarget;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Human-readable, line-oriented filter-table format.
 *
 * <p>Each entry starts with {@code allow} or {@code deny}, followed by {@code address} and an
 * IP address or CIDR, or {@code subject} and its type and ID. Optional {@code until} and
 * {@code reason} fields follow the target. Values containing whitespace are quoted. Quoted
 * values support backslash escapes for quotes, backslashes, and control characters. A hash at
 * the start of a token begins a comment.</p>
 *
 * @author RollW
 */
public final class LineFilterTableFileFormat implements FilterTableFileFormat {
    private static final String HEADER = "# Luxspec firewall filter table";
    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    @Override
    public FilterTableSnapshot read(BufferedReader reader, String sourceName) throws IOException {
        BufferedReader nonNullReader = Objects.requireNonNull(reader, "reader");
        String nonNullSourceName = Objects.requireNonNull(sourceName, "sourceName");
        List<FilterEntry> entries = new ArrayList<>();
        String line;
        int lineNumber = 0;
        while ((line = nonNullReader.readLine()) != null) {
            lineNumber++;
            String content = line.stripLeading();
            if (content.isBlank() || content.startsWith("#")) {
                continue;
            }
            try {
                entries.add(parseEntry(line));
            } catch (RuntimeException exception) {
                throw invalidLine(nonNullSourceName, lineNumber, exception);
            }
        }
        try {
            return new FilterTableSnapshot(entries);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Invalid filter table contents: " + nonNullSourceName, exception);
        }
    }

    @Override
    public void write(Writer writer, FilterTableSnapshot snapshot) throws IOException {
        Writer nonNullWriter = Objects.requireNonNull(writer, "writer");
        FilterTableSnapshot nonNullSnapshot = Objects.requireNonNull(snapshot, "snapshot");
        nonNullWriter.write(HEADER);
        nonNullWriter.write('\n');
        nonNullWriter.write("# allow|deny address <address> [until <instant>] [reason <text>]\n");
        nonNullWriter.write("# allow|deny subject <type> <id> [until <instant>] [reason <text>]\n");
        for (FilterEntry entry : nonNullSnapshot.getEntries()) {
            nonNullWriter.write(formatEntry(entry));
            nonNullWriter.write('\n');
        }
    }

    private static FilterEntry parseEntry(String line) {
        List<String> tokens = tokenize(line);
        if (tokens.size() < 3) {
            throw new IllegalArgumentException("expected a mode, target type, and target value");
        }
        FilterMode mode = FilterMode.fromString(tokens.get(0));
        String targetType = tokens.get(1).toLowerCase(Locale.ROOT);
        FilterTarget target;
        int index;
        if (targetType.equals("address")) {
            target = FilterTarget.clientAddress(IpNetwork.parse(tokens.get(2)));
            index = 3;
        } else if (targetType.equals("subject")) {
            if (tokens.size() < 4) {
                throw new IllegalArgumentException("subject requires a type and ID");
            }
            target = FilterTarget.subject(new SimpleSubject(tokens.get(2), tokens.get(3)));
            index = 4;
        } else {
            throw new IllegalArgumentException("unknown target type: " + targetType);
        }
        Instant expiresAt = null;
        String reason = null;
        while (index < tokens.size()) {
            String name = tokens.get(index++).toLowerCase(Locale.ROOT);
            if (index >= tokens.size()) {
                throw new IllegalArgumentException("missing value for " + name);
            }
            String value = tokens.get(index++);
            if (name.equals("until") && expiresAt == null) {
                expiresAt = Instant.parse(value);
            } else if (name.equals("reason") && reason == null) {
                reason = value;
            } else {
                throw new IllegalArgumentException("unknown or repeated field: " + name);
            }
        }
        return new FilterEntry(target, mode, expiresAt, reason);
    }

    private static List<String> tokenize(String line) {
        List<String> tokens = new ArrayList<>();
        int index = 0;
        while (index < line.length()) {
            while (index < line.length() && Character.isWhitespace(line.charAt(index))) {
                index++;
            }
            if (index == line.length() || line.charAt(index) == '#') {
                break;
            }
            StringBuilder token = new StringBuilder();
            if (line.charAt(index) == '"') {
                index = readQuoted(line, index + 1, token);
                if (index < line.length() && !Character.isWhitespace(line.charAt(index))) {
                    throw new IllegalArgumentException("quoted value must end at a token boundary");
                }
            } else {
                while (index < line.length() && !Character.isWhitespace(line.charAt(index))) {
                    token.append(line.charAt(index++));
                }
            }
            tokens.add(validText(token.toString()));
        }
        return tokens;
    }

    private static int readQuoted(String line, int index, StringBuilder value) {
        while (index < line.length()) {
            char character = line.charAt(index++);
            if (character == '"') {
                return index;
            }
            if (character != '\\') {
                value.append(character);
                continue;
            }
            if (index == line.length()) {
                throw new IllegalArgumentException("incomplete escape");
            }
            char escaped = line.charAt(index++);
            switch (escaped) {
                case '\\', '"' -> value.append(escaped);
                case 'n' -> value.append('\n');
                case 'r' -> value.append('\r');
                case 't' -> value.append('\t');
                case 'u' -> {
                    if (index + 4 > line.length()) {
                        throw new IllegalArgumentException("incomplete character escape");
                    }
                    int code = 0;
                    for (int end = index + 4; index < end; index++) {
                        int digit = Character.digit(line.charAt(index), 16);
                        if (digit < 0) {
                            throw new IllegalArgumentException("invalid character escape");
                        }
                        code = (code << 4) | digit;
                    }
                    value.append((char) code);
                }
                default -> throw new IllegalArgumentException("unknown escape: " + escaped);
            }
        }
        throw new IllegalArgumentException("unterminated quoted value");
    }

    private static String formatEntry(FilterEntry entry) {
        StringBuilder result = new StringBuilder(entry.mode().name().toLowerCase(Locale.ROOT));
        FilterTarget target = entry.target();
        if (target instanceof FilterTarget.ClientAddressTarget address) {
            result.append(" address ").append(address.address());
        } else if (target instanceof FilterTarget.SubjectTarget subject) {
            result.append(" subject ");
            appendQuoted(result, subject.subject().type());
            result.append(' ');
            appendQuoted(result, subject.subject().id());
        } else {
            throw new IllegalArgumentException("Unsupported filter target: " + target.getClass().getName());
        }
        if (entry.expiresAt() != null) {
            result.append(" until ").append(entry.expiresAt());
        }
        if (entry.reason() != null) {
            result.append(" reason ");
            appendQuoted(result, entry.reason());
        }
        return result.toString();
    }

    private static void appendQuoted(StringBuilder result, String text) {
        result.append('"');
        String value = validText(text);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> result.append("\\\\");
                case '"' -> result.append("\\\"");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (Character.isISOControl(character)) {
                        result.append("\\u");
                        result.append(HEX_DIGITS[(character >>> 12) & 0xf]);
                        result.append(HEX_DIGITS[(character >>> 8) & 0xf]);
                        result.append(HEX_DIGITS[(character >>> 4) & 0xf]);
                        result.append(HEX_DIGITS[character & 0xf]);
                    } else {
                        result.append(character);
                    }
                }
            }
        }
        result.append('"');
    }

    private static String validText(String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isHighSurrogate(character)) {
                if (++index == value.length() || !Character.isLowSurrogate(value.charAt(index))) {
                    throw new IllegalArgumentException("value contains invalid UTF-16");
                }
            } else if (Character.isLowSurrogate(character)) {
                throw new IllegalArgumentException("value contains invalid UTF-16");
            }
        }
        return value;
    }

    private static IllegalStateException invalidLine(String sourceName, int lineNumber, RuntimeException cause) {
        return new IllegalStateException(
                "Invalid filter table line " + lineNumber + " in " + sourceName + ": " + cause.getMessage(),
                cause
        );
    }
}
