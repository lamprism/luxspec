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

package com.lamprism.luxspec.resource.spring;

import com.lamprism.luxspec.resource.ResourceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UlidResourceIdGeneratorTest {
    private static final ResourceType<String> ARTICLE = ResourceType.of("article", String.class);

    @Test
    void generatesCanonicalUniqueUlids() {
        UlidResourceIdGenerator generator = new UlidResourceIdGenerator();

        String first = generator.nextId(ARTICLE);
        String second = generator.nextId(ARTICLE);

        assertEquals(26, first.length());
        assertEquals(26, second.length());
        assertNotEquals(first, second);
        assertTrue(isCrockfordBase32(first));
        assertTrue(isCrockfordBase32(second));
    }

    @Test
    void rejectsMissingResourceType() {
        UlidResourceIdGenerator generator = new UlidResourceIdGenerator();

        assertThrows(NullPointerException.class, () -> generator.nextId(null));
    }

    private static boolean isCrockfordBase32(String value) {
        String alphabet = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
        for (int index = 0; index < value.length(); index++) {
            if (alphabet.indexOf(value.charAt(index)) < 0) {
                return false;
            }
        }
        return true;
    }
}
