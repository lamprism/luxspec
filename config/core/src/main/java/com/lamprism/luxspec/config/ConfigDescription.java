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

package com.lamprism.luxspec.config;

import com.lamprism.luxspec.message.LocalizedText;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;

/**
 * Describes a configuration definition with optional local translations.
 *
 * @author RollW
 */
public final class ConfigDescription {
    public static final ConfigDescription EMPTY = new ConfigDescription();

    // TODO: use interface for LocalizedText
    private final @Nullable LocalizedText text;

    private ConfigDescription() {
        this.text = null;
    }

    private ConfigDescription(LocalizedText text) {
        this.text = Objects.requireNonNull(text, "text");
    }

    private ConfigDescription(String text) {
        this(LocalizedText.of(text));
    }

    /**
     * Creates an inline description.
     *
     * @param value the description text
     * @return the inline description
     */
    static ConfigDescription text(String value) {
        return new ConfigDescription(value);
    }

    /**
     * Creates a description with local translations.
     *
     * @param text the immutable local text
     * @return the localized description
     */
    static ConfigDescription localized(LocalizedText text) {
        return new ConfigDescription(text);
    }

    /**
     * Resolves this description for a presentation locale.
     *
     * @param locale the requested locale
     * @return the display text
     */
    public String resolve(Locale locale) {
        Locale nonNullLocale = Objects.requireNonNull(locale, "locale");
        if (text == null) {
            return "";
        }
        return text.resolve(nonNullLocale);
    }

    /**
     * Returns the local text used by this description.
     *
     * @return the immutable localized text, or {@code null} when the description is empty
     */
    public @Nullable LocalizedText getText() {
        return text;
    }
}
