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
 * Immutable text with local translations and an explicit default.
 *
 * <p>Use this value for library text published together with its owner. The requested locale is
 * matched exactly, then by language, before the default text is returned.</p>
 *
 * @author RollW
 */
public final class LocalizedText {
    private final String defaultText;
    private final Map<Locale, String> translations;

    private LocalizedText(String defaultText, Map<Locale, String> translations) {
        this.defaultText = requireText(defaultText, "defaultText");
        this.translations = Map.copyOf(Objects.requireNonNull(translations, "translations"));
    }

    /**
     * Creates text with no locale-specific translation.
     *
     * @param defaultText the non-blank text returned when no translation matches
     * @return the localized text
     */
    public static LocalizedText of(String defaultText) {
        return new LocalizedText(defaultText, Map.of());
    }

    /**
     * Creates text with one locale-specific translation.
     *
     * @param defaultText the non-blank text returned when no translation matches
     * @param locale      the translated locale
     * @param translation the non-blank translation
     * @return the localized text
     */
    public static LocalizedText of(String defaultText, Locale locale, String translation) {
        return builder(defaultText).translation(locale, translation).build();
    }

    /**
     * Creates a builder for text with multiple locale-specific translations.
     *
     * @param defaultText the non-blank text returned when no translation matches
     * @return the translation builder
     */
    public static Builder builder(String defaultText) {
        return new Builder(defaultText);
    }

    /**
     * Resolves text for a requested locale.
     *
     * @param locale the requested locale
     * @return an exact translation, a language translation, or the default text
     */
    public String resolve(Locale locale) {
        Locale nonNullLocale = Objects.requireNonNull(locale, "locale");
        String exactTranslation = translations.get(nonNullLocale);
        if (exactTranslation != null) {
            return exactTranslation;
        }
        String language = nonNullLocale.getLanguage();
        if (!language.isBlank()) {
            String languageTranslation = translations.get(Locale.forLanguageTag(language));
            if (languageTranslation != null) {
                return languageTranslation;
            }
        }
        return defaultText;
    }

    /**
     * Returns the default text.
     *
     * @return the non-blank default text
     */
    public String getDefaultText() {
        return defaultText;
    }

    /**
     * Builds immutable local translations.
     *
     * @author RollW
     */
    public static final class Builder {
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
                throw new IllegalArgumentException("A translation is already defined for " + nonNullLocale);
            }
            return this;
        }

        /**
         * Builds immutable localized text.
         *
         * @return the localized text
         */
        public LocalizedText build() {
            return new LocalizedText(defaultText, translations);
        }
    }

    private static String requireText(String value, String name) {
        String nonNullValue = Objects.requireNonNull(value, name).trim();
        if (nonNullValue.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return nonNullValue;
    }
}
