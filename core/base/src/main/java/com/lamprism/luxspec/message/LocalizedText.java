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

package com.lamprism.luxspec.message;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Provider-independent text that resolves itself for a requested locale.
 *
 * <p>The factory and builder create immutable inline text for metadata that is published with its
 * owner. Key-based messages remain represented by {@link MessageResource} and resolved through
 * {@link MessageResolver}.</p>
 *
 * @author RollW
 */
public interface LocalizedText {
    /**
     * Resolves this text for a requested locale.
     *
     * @param locale the requested locale
     * @return the best matching translation or the default text
     */
    String resolve(Locale locale);

    /**
     * Returns the default text used when no translation matches.
     *
     * @return the non-blank default text
     */
    default String getDefaultText() {
        return resolve(Locale.ROOT);
    }

    /**
     * Creates text with no locale-specific translation.
     *
     * @param defaultText the non-blank text returned when no translation matches
     * @return the localized text
     */
    static LocalizedText of(String defaultText) {
        return new ImmutableLocalizedText(defaultText, Map.of());
    }

    /**
     * Creates text with one locale-specific translation.
     *
     * @param defaultText the non-blank text returned when no translation matches
     * @param locale      the translated locale
     * @param translation the non-blank translation
     * @return the localized text
     */
    static LocalizedText of(String defaultText, Locale locale, String translation) {
        return builder(defaultText).translation(locale, translation).build();
    }

    /**
     * Creates a builder for text with multiple locale-specific translations.
     *
     * @param defaultText the non-blank text returned when no translation matches
     * @return the translation builder
     */
    static Builder builder(String defaultText) {
        return new Builder(defaultText);
    }

    /**
     * Builds immutable local translations.
     *
     * @author RollW
     */
    final class Builder {
        private final String defaultText;
        private final Map<Locale, String> translations = new LinkedHashMap<>();

        private Builder(String defaultText) {
            this.defaultText = requireText(defaultText, "defaultText");
        }

        /**
         * Adds one locale-specific translation.
         *
         * @param locale      the translated locale
         * @param translation the non-blank translation
         * @return this builder
         */
        public Builder translation(Locale locale, String translation) {
            Locale nonNullLocale = Objects.requireNonNull(locale, "locale");
            String nonBlankTranslation = requireText(translation, "translation");
            if (translations.putIfAbsent(nonNullLocale, nonBlankTranslation) != null) {
                throw new IllegalArgumentException(
                        "A translation is already defined for " + nonNullLocale
                );
            }
            return this;
        }

        /**
         * Builds immutable localized text.
         *
         * @return the localized text
         */
        public LocalizedText build() {
            return new ImmutableLocalizedText(defaultText, translations);
        }

        private static String requireText(String value, String name) {
            String nonNullValue = Objects.requireNonNull(value, name).trim();
            if (nonNullValue.isEmpty()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return nonNullValue;
        }
    }
}
