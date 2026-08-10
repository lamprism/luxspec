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

package com.lamprism.luxspec.data.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryFieldTest {
    @Test
    void acceptsDomainNamesThatAreNotJavaIdentifiers() {
        assertDoesNotThrow(() -> QueryField.of("1st--party.name", String.class));
        assertDoesNotThrow(() -> QueryField.of("display/name", String.class));
    }

    @Test
    void rejectsEmptyWhitespaceAndControlNames() {
        assertThrows(IllegalArgumentException.class, () -> QueryField.of("", String.class));
        assertThrows(IllegalArgumentException.class, () -> QueryField.of("display name", String.class));
        assertThrows(IllegalArgumentException.class, () -> QueryField.of("display\nname", String.class));
    }
}
