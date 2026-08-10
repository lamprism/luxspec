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

package com.lamprism.luxspec.validation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidationRulesTest {
    @Test
    void composesTextRulesWithoutNormalizingTheValue() {
        Validator<String> validator = ValidationRules
                .nonBlank("name")
                .and(ValidationRules.noWhitespace("name"))
                .and(ValidationRules.noControlCharacters("name"));

        assertDoesNotThrow(() -> validator.validate("order.created"));
        assertThrows(ValidationException.class, () -> validator.validate("order created"));
        assertThrows(ValidationException.class, () -> validator.validate("order\ncreated"));
    }

    @Test
    void validatesListElementsWithTheSameRuleContract() {
        Validator<List<String>> validator = ValidationRules.elements(
                ValidationRules.nonBlank("element")
        );

        assertDoesNotThrow(() -> validator.validate(List.of("one", "two")));
        assertThrows(ValidationException.class, () -> validator.validate(List.of("one", " ")));
    }

    @Test
    void validatesLengthAndMembershipRules() {
        Validator<String> length = ValidationRules.maxLength("name", 4);
        Validator<String> membership = ValidationRules.oneOf("kind", Set.of("user", "service"));

        assertDoesNotThrow(() -> length.validate("name"));
        assertThrows(ValidationException.class, () -> length.validate("names"));
        assertDoesNotThrow(() -> membership.validate("user"));
        assertThrows(ValidationException.class, () -> membership.validate("system"));
    }

    @Test
    void rejectsNullValuesBeforeApplyingRules() {
        Validator<String> validator = ValidationRules.nonBlank("name");

        assertThrows(NullPointerException.class, () -> Validator.<String>none().validate(null));
        assertThrows(NullPointerException.class, () -> validator.validate(null));
    }

    @Test
    void rejectsInvalidRuleDefinitions() {
        assertThrows(IllegalArgumentException.class, () -> ValidationRules.nonBlank(" "));
        assertThrows(IllegalArgumentException.class, () -> ValidationRules.maxLength("name", 0));
        assertThrows(IllegalArgumentException.class, () -> ValidationRules.oneOf("kind", Set.of()));
    }
}
