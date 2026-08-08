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

package com.lamprism.luxspec.data.jpa.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Stores locales as canonical BCP 47 language tags.
 *
 * @author RollW
 */
@Converter(autoApply = true)
public final class LocaleAttributeConverter implements AttributeConverter<Locale, String> {
    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable Locale attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.toLanguageTag();
    }

    @Override
    public @Nullable Locale convertToEntityAttribute(@Nullable String dbData) {
        if (dbData == null) {
            return null;
        }
        return Locale.forLanguageTag(dbData);
    }
}
