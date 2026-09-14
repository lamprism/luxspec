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

package com.lamprism.luxspec.message.toml;

import com.lamprism.luxspec.message.MessageResolver;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.dataformat.toml.TomlMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves messages from explicitly registered UTF-8 TOML files.
 *
 * <p>Each file is loaded into an immutable snapshot when this resolver is built. TOML tables
 * organize keys, and textual leaves become dot-separated message keys. For example,
 * {@code [error]} with {@code denied = "Denied"} defines {@code error.denied}. TOML comments
 * can document translation context without becoming part of the message data.</p>
 *
 * @author RollW
 */
public class TomlMessageResolver implements MessageResolver {
    private static final TomlMapper TOML_MAPPER = TomlMapper.builder().build();

    private final Locale fallbackLocale;
    private final Map<Locale, Map<String, String>> messagesByLocale;
    private final String missingMessage;

    private TomlMessageResolver(
            Locale fallbackLocale,
            Map<Locale, Map<String, String>> messagesByLocale,
            String missingMessage
    ) {
        this.fallbackLocale = Objects.requireNonNull(fallbackLocale, "fallbackLocale");
        this.messagesByLocale = Map.copyOf(Objects.requireNonNull(messagesByLocale, "messagesByLocale"));
        this.missingMessage = requireText(missingMessage, "missingMessage");
    }

    /**
     * Creates a resolver builder with an explicit fallback locale.
     *
     * @param fallbackLocale the locale used after requested locale lookup fails
     * @param missingMessage the non-blank result returned when no registered file defines a key
     * @return the file registration builder
     */
    public static Builder builder(Locale fallbackLocale, String missingMessage) {
        return new Builder(fallbackLocale, missingMessage);
    }

    @Override
    public String resolve(String key, Locale locale, Object... arguments) {
        String nonBlankKey = requireText(key, "key");
        Locale nonNullLocale = Objects.requireNonNull(locale, "locale");
        Object[] nonNullArguments = Objects.requireNonNull(arguments, "arguments");
        String pattern = findMessage(nonBlankKey, nonNullLocale);
        if (pattern == null) {
            return missingMessage;
        }
        try {
            return new MessageFormat(pattern, nonNullLocale).format(nonNullArguments);
        } catch (IllegalArgumentException exception) {
            return missingMessage;
        }
    }

    private @Nullable String findMessage(String key, Locale requestedLocale) {
        String message = findMessage(Locale.forLanguageTag(fallbackLocale.getLanguage()), key);
        message = findMessage(fallbackLocale, key, message);
        message = findMessage(Locale.forLanguageTag(requestedLocale.getLanguage()), key, message);
        return findMessage(requestedLocale, key, message);
    }

    private @Nullable String findMessage(Locale locale, String key) {
        return findMessage(locale, key, null);
    }

    private @Nullable String findMessage(Locale locale, String key, @Nullable String fallback) {
        Map<String, String> messages = messagesByLocale.get(locale);
        if (messages == null) {
            return fallback;
        }
        return messages.getOrDefault(key, fallback);
    }

    /**
     * Builds an immutable resolver from explicit locale-file registrations.
     *
     * @author RollW
     */
    public static final class Builder {
        private final Locale fallbackLocale;
        private final String missingMessage;
        private final Map<Locale, Path> filesByLocale = new LinkedHashMap<>();

        private Builder(Locale fallbackLocale, String missingMessage) {
            this.fallbackLocale = Objects.requireNonNull(fallbackLocale, "fallbackLocale");
            this.missingMessage = requireText(missingMessage, "missingMessage");
        }

        /**
         * Registers one TOML file for a locale.
         *
         * @param locale the locale represented by the file
         * @param path   the UTF-8 TOML file to load
         * @return this builder
         */
        public Builder file(Locale locale, Path path) {
            Locale nonNullLocale = Objects.requireNonNull(locale, "locale");
            Path nonNullPath = Objects.requireNonNull(path, "path");
            if (filesByLocale.putIfAbsent(nonNullLocale, nonNullPath) != null) {
                throw new IllegalArgumentException("A message file is already registered for " + nonNullLocale);
            }
            return this;
        }

        /**
         * Loads every registered file and builds the resolver.
         *
         * @return the immutable file-backed resolver
         */
        public TomlMessageResolver build() {
            if (filesByLocale.isEmpty()) {
                throw new IllegalStateException("At least one message file must be registered");
            }
            Map<Locale, Map<String, String>> messagesByLocale = new LinkedHashMap<>();
            for (Map.Entry<Locale, Path> entry : filesByLocale.entrySet()) {
                messagesByLocale.put(entry.getKey(), readMessages(entry.getValue()));
            }
            return new TomlMessageResolver(fallbackLocale, messagesByLocale, missingMessage);
        }
    }

    private static Map<String, String> readMessages(Path path) {
        try (var input = Files.newInputStream(path)) {
            Map<?, ?> document = TOML_MAPPER.readValue(input, Map.class);
            if (document == null) {
                throw new IllegalArgumentException("Message TOML document must contain a table");
            }
            Map<String, String> messages = new LinkedHashMap<>();
            readTable(document, "", messages);
            return Map.copyOf(messages);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Unable to parse message TOML file: " + path, exception);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read message TOML file: " + path, exception);
        }
    }

    private static void readTable(Map<?, ?> table, String prefix, Map<String, String> messages) {
        for (Map.Entry<?, ?> entry : table.entrySet()) {
            if (!(entry.getKey() instanceof String name)) {
                throw new IllegalArgumentException("Message TOML table contains a non-string key");
            }
            String segment = requireSegment(name);
            String key = prefix.isEmpty() ? segment : prefix + "." + segment;
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nestedTable) {
                readTable(nestedTable, key, messages);
                continue;
            }
            if (!(value instanceof String text)) {
                throw new IllegalArgumentException("Message TOML value for " + key + " must be a string");
            }
            String nonBlankText = requireText(text, "Message TOML value for " + key);
            if (messages.putIfAbsent(key, nonBlankText) != null) {
                throw new IllegalArgumentException("Message TOML contains a duplicate key: " + key);
            }
        }
    }

    private static String requireSegment(String value) {
        String nonBlankValue = requireText(value, "Message TOML key");
        if (nonBlankValue.indexOf('.') >= 0) {
            throw new IllegalArgumentException("Message TOML key must not contain a period: " + nonBlankValue);
        }
        return nonBlankValue;
    }

    private static String requireText(String value, String name) {
        String nonNullValue = Objects.requireNonNull(value, name).trim();
        if (nonNullValue.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return nonNullValue;
    }
}
