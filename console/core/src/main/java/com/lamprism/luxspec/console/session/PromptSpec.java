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

package com.lamprism.luxspec.console.session;

import com.lamprism.luxspec.console.ValueParser;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Immutable typed description of one runtime prompt.
 *
 * @author RollW
 */
public final class PromptSpec<T> {
    /**
     * Prompt presentation modes understood by session adapters.
     */
    public enum Kind {
        /**
         * A normal visible text prompt.
         */
        TEXT,
        /**
         * A prompt whose value should not be displayed.
         */
        SECRET,
        /**
         * A boolean confirmation prompt.
         */
        CONFIRM
    }

    private final String message;
    private final Kind kind;
    private final ValueParser<T> parser;
    private final @Nullable T defaultValue;

    private PromptSpec(String message,
                       Kind kind,
                       ValueParser<T> parser,
                       @Nullable T defaultValue) {
        this.message = requireMessage(message);
        this.kind = Objects.requireNonNull(kind, "kind");
        this.parser = Objects.requireNonNull(parser, "parser");
        if (kind == Kind.SECRET && defaultValue != null) {
            throw new IllegalArgumentException("A secret prompt cannot define a default value");
        }
        this.defaultValue = defaultValue;
    }

    /**
     * Creates a text prompt returning the entered String.
     *
     * @param message prompt message
     * @return a text prompt specification
     */
    public static PromptSpec<String> text(String message) {
        return new PromptSpec<>(message, Kind.TEXT, ValueParser.string(), null);
    }

    /**
     * Creates a secret prompt returning entered characters.
     *
     * @param message prompt message
     * @return a secret prompt specification
     */
    public static PromptSpec<char[]> secret(String message) {
        return new PromptSpec<>(message, Kind.SECRET, PromptSpec::parseSecret, null);
    }

    /**
     * Creates a boolean confirmation prompt.
     *
     * @param message      prompt message
     * @param defaultValue value used when the provider accepts the default
     * @return a confirmation prompt specification
     */
    public static PromptSpec<Boolean> confirm(String message, boolean defaultValue) {
        return new PromptSpec<>(message, Kind.CONFIRM, ValueParser.booleanValue(), defaultValue);
    }

    /**
     * Creates a text-mode prompt with an application-defined parser.
     *
     * @param message prompt message
     * @param parser  parser for accepted prompt text
     * @param <T>     accepted value type
     * @return a typed prompt specification
     */
    public static <T> PromptSpec<T> value(String message, ValueParser<T> parser) {
        return new PromptSpec<>(message, Kind.TEXT, parser, null);
    }

    /**
     * @return prompt text
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return prompt presentation kind
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * @return parser for provider-entered text
     */
    public ValueParser<T> getParser() {
        return parser;
    }

    /**
     * @return whether a default value exists
     */
    public boolean hasDefaultValue() {
        return defaultValue != null;
    }

    /**
     * @return the default value, or {@code null} when absent
     */
    public @Nullable T getDefaultValue() {
        return defaultValue;
    }

    /**
     * Returns a copy with an explicit default value.
     *
     * @param value default value
     * @return a prompt specification with the default value
     */
    public PromptSpec<T> withDefault(T value) {
        if (kind == Kind.SECRET) {
            throw new IllegalArgumentException("A secret prompt cannot define a default value");
        }
        return new PromptSpec<>(message, kind, parser, Objects.requireNonNull(value, "value"));
    }

    private static char[] parseSecret(String token) {
        return token.toCharArray();
    }

    private static String requireMessage(String message) {
        Objects.requireNonNull(message, "message");
        String normalized = message.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Prompt message cannot be blank");
        }
        return normalized;
    }
}
