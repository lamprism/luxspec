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


import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable inline implementation of {@link LocalizedText}.
 *
 * @author RollW
 */
public final class ImmutableLocalizedText implements LocalizedText {
    private final String defaultText;
    private final Map<Locale, String> translations;

    ImmutableLocalizedText(String defaultText, Map<Locale, String> translations) {
        this.defaultText = requireText(defaultText, "defaultText");
        this.translations = Map.copyOf(Objects.requireNonNull(translations, "translations"));
    }

    @Override
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

    @Override
    public String getDefaultText() {
        return defaultText;
    }

    private static String requireText(String value, String name) {
        String nonNullValue = Objects.requireNonNull(value, name).trim();
        if (nonNullValue.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return nonNullValue;
    }
}
