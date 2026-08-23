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

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalizedTextTest {
    @Test
    void resolvesExactLanguageAndDefaultTranslations() {
        LocalizedText text = LocalizedText.builder("Default")
                .translation(Locale.FRENCH, "Francais")
                .translation(Locale.CANADA_FRENCH, "Francais canadien")
                .build();

        assertEquals("Francais canadien", text.resolve(Locale.CANADA_FRENCH));
        assertEquals("Francais", text.resolve(Locale.FRANCE));
        assertEquals("Default", text.resolve(Locale.US));
    }

    @Test
    void rejectsDuplicateLocaleTranslations() {
        LocalizedText.Builder builder = LocalizedText.builder("Default")
                .translation(Locale.FRENCH, "Francais");

        assertThrows(
                IllegalArgumentException.class,
                () -> builder.translation(Locale.FRENCH, "French")
        );
    }

    @Test
    void acceptsCustomProviderIndependentTextImplementations() {
        LocalizedText text = locale -> locale.getLanguage().equals(Locale.FRENCH.getLanguage())
                ? "Francais"
                : "Default";

        assertEquals("Francais", text.resolve(Locale.FRANCE));
        assertEquals("Default", text.getDefaultText());
    }
}
