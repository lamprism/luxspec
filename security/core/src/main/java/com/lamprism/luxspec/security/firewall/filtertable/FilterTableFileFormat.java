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

import com.lamprism.luxspec.security.authentication.SimpleSubject;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntPredicate;

/**
 * Reads and writes the human-editable filter-table TSV representation.
 *
 * <p>This class owns the text format only. The caller owns the supplied reader or writer and is
 * responsible for file paths, lifecycle, and atomic replacement.</p>
 *
 * @author RollW
 */
final class FilterTableFileFormat {
    private static final String HEADER = "# Luxspec firewall filter table v1";
    private static final String COLUMNS = "# target\tvalue\tmode\texpiresAt\treason";
    private static final String NO_EXPIRATION = "-";

    private FilterTableFileFormat() {
    }

    static FilterTableSnapshot read(BufferedReader reader, String sourceName) throws IOException {
        BufferedReader nonNullReader = Objects.requireNonNull(reader, "reader");
        String nonNullSourceName = Objects.requireNonNull(sourceName, "sourceName");
        List<FilterEntry> entries = new ArrayList<>();
        String line;
        int lineNumber = 0;
        while ((line = nonNullReader.readLine()) != null) {
            lineNumber++;
            if (isIgnorable(line)) {
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

    static void write(Writer writer, FilterTableSnapshot snapshot) throws IOException {
        Writer nonNullWriter = Objects.requireNonNull(writer, "writer");
        FilterTableSnapshot nonNullSnapshot = Objects.requireNonNull(snapshot, "snapshot");
        nonNullWriter.write(HEADER);
        nonNullWriter.write('\n');
        nonNullWriter.write(COLUMNS);
        nonNullWriter.write('\n');
        for (FilterEntry entry : nonNullSnapshot.getEntries()) {
            nonNullWriter.write(formatEntry(entry));
            nonNullWriter.write('\n');
        }
    }

    private static boolean isIgnorable(String line) {
        if (line.isBlank()) {
            return true;
        }
        return line.stripLeading().startsWith("#");
    }

    private static FilterEntry parseEntry(String line) {
        List<String> fields = splitFields(line);
        if (fields.size() != 5) {
            throw new IllegalArgumentException("expected five tab-separated fields");
        }
        FilterTarget target = parseTarget(fields.get(0), fields.get(1));
        FilterMode mode = FilterMode.fromString(fields.get(2));
        Instant expiresAt = parseExpiration(fields.get(3));
        String reason = parseReason(fields.get(4));
        return new FilterEntry(target, mode, expiresAt, reason);
    }

    private static FilterTarget parseTarget(String targetType, String encodedValue) {
        return switch (targetType) {
            case "ADDRESS" -> FilterTarget.clientAddress(decode(encodedValue, "target value"));
            case "SUBJECT" -> parseSubject(encodedValue);
            default -> throw new IllegalArgumentException("unknown target type: " + targetType);
        };
    }

    private static FilterTarget parseSubject(String encodedValue) {
        int separator = encodedValue.indexOf(':');
        if (separator < 1 || separator == encodedValue.length() - 1
                || separator != encodedValue.lastIndexOf(':')) {
            throw new IllegalArgumentException("subject target must contain one type/id separator");
        }
        String type = decode(encodedValue.substring(0, separator), "subject type");
        String id = decode(encodedValue.substring(separator + 1), "subject id");
        return FilterTarget.subject(new SimpleSubject(type, id));
    }

    private static @Nullable Instant parseExpiration(String value) {
        if (NO_EXPIRATION.equals(value)) {
            return null;
        }
        return Instant.parse(value);
    }

    private static @Nullable String parseReason(String encodedValue) {
        if (encodedValue.isEmpty()) {
            return null;
        }
        return decode(encodedValue, "reason");
    }

    private static List<String> splitFields(String line) {
        List<String> fields = new ArrayList<>(5);
        int fieldStart = 0;
        for (int index = 0; index < line.length(); index++) {
            if (line.charAt(index) != '\t') {
                continue;
            }
            fields.add(line.substring(fieldStart, index));
            fieldStart = index + 1;
        }
        fields.add(line.substring(fieldStart));
        return fields;
    }

    private static String formatEntry(FilterEntry entry) {
        StringBuilder result = new StringBuilder();
        FilterTarget target = entry.target();
        if (target instanceof FilterTarget.ClientAddressTarget addressTarget) {
            result.append("ADDRESS").append('\t');
            result.append(encodeAddress(addressTarget.address()));
        } else if (target instanceof FilterTarget.SubjectTarget subjectTarget) {
            result.append("SUBJECT").append('\t');
            result.append(encode(subjectTarget.subject().type()));
            result.append(':');
            result.append(encode(subjectTarget.subject().id()));
        } else {
            throw new IllegalArgumentException("Unsupported filter target: " + target.getClass().getName());
        }
        result.append('\t').append(entry.mode().name());
        result.append('\t').append(entry.expiresAt() == null ? NO_EXPIRATION : entry.expiresAt());
        result.append('\t');
        if (entry.reason() != null) {
            result.append(encode(entry.reason()));
        }
        return result.toString();
    }

    private static String encode(String value) {
        return encode(value, FilterTableFileFormat::isSafe);
    }

    private static String encodeAddress(String value) {
        return encode(value, FilterTableFileFormat::isAddressSafe);
    }

    private static String encode(String value, IntPredicate safeByte) {
        byte[] bytes = encodeUtf8(value);
        StringBuilder result = new StringBuilder(bytes.length);
        for (byte valueByte : bytes) {
            int unsignedByte = valueByte & 0xff;
            if (safeByte.test(unsignedByte)) {
                result.append((char) unsignedByte);
            } else {
                result.append('%');
                result.append(HEX_DIGITS[unsignedByte >>> 4]);
                result.append(HEX_DIGITS[unsignedByte & 0x0f]);
            }
        }
        return result.toString();
    }

    private static String decode(String value, String fieldName) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '%') {
                if (index + 2 >= value.length()) {
                    throw new IllegalArgumentException(fieldName + " has an incomplete percent escape");
                }
                int high = hexValue(value.charAt(index + 1));
                int low = hexValue(value.charAt(index + 2));
                bytes.write((high << 4) | low);
                index += 2;
                continue;
            }
            if (character > 0x7f) {
                throw new IllegalArgumentException(fieldName + " must use percent encoding for non-ASCII text");
            }
            bytes.write(character);
        }
        try {
            var decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            return decoder.decode(ByteBuffer.wrap(bytes.toByteArray())).toString();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException(fieldName + " contains invalid UTF-8", exception);
        }
    }

    private static byte[] encodeUtf8(String value) {
        try {
            var encoder = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            ByteBuffer encoded = encoder.encode(CharBuffer.wrap(value));
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            return bytes;
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("Value contains invalid UTF-16", exception);
        }
    }

    private static boolean isSafe(int value) {
        return value >= 'A' && value <= 'Z'
                || value >= 'a' && value <= 'z'
                || value >= '0' && value <= '9'
                || value == '.'
                || value == '_'
                || value == '-'
                || value == '~';
    }

    private static boolean isAddressSafe(int value) {
        return isSafe(value) || value == ':' || value == '/';
    }

    private static int hexValue(char value) {
        if (value >= '0' && value <= '9') {
            return value - '0';
        }
        if (value >= 'A' && value <= 'F') {
            return value - 'A' + 10;
        }
        if (value >= 'a' && value <= 'f') {
            return value - 'a' + 10;
        }
        throw new IllegalArgumentException("Invalid percent escape");
    }

    private static IllegalStateException invalidLine(
            String sourceName,
            int lineNumber,
            RuntimeException cause
    ) {
        return new IllegalStateException(
                "Invalid filter table line " + lineNumber + " in " + sourceName + ": " + cause.getMessage(),
                cause
        );
    }

    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();
}
