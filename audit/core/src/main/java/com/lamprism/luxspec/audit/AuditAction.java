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

package com.lamprism.luxspec.audit;

import com.lamprism.luxspec.validation.ValidationRules;
import com.lamprism.luxspec.validation.Validator;

import java.util.Objects;

/**
 * Extensible semantic audit action.
 *
 * <p>{@link #of(String)} trims surrounding characters, rejects null or blank
 * values, and rejects whitespace, space characters, and ISO control characters.
 * The remaining value is preserved exactly. Audit does not impose a fixed
 * action vocabulary.
 *
 * @author RollW
 */
public final class AuditAction {
    private static final Validator<String> VALUE_VALIDATOR = ValidationRules
            .nonBlank("Audit action")
            .and(ValidationRules.noWhitespace("Audit action"))
            .and(ValidationRules.noControlCharacters("Audit action"));

    private final String value;

    private AuditAction(String value) {
        this.value = value;
    }

    public static AuditAction of(String value) {
        String normalized = Objects.requireNonNull(value, "value").trim();
        VALUE_VALIDATOR.validate(normalized);
        return new AuditAction(normalized);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AuditAction action)) {
            return false;
        }
        return value.equals(action.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
