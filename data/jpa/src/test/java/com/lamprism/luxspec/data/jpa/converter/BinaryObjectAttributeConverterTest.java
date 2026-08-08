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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BinaryObjectAttributeConverterTest {
    private final TestConverter converter = new TestConverter();

    @Test
    void roundTripsAnObjectThroughVersionedCompressedCbor() {
        Payload original = new Payload("alpha", 7);

        byte[] stored = converter.convertToDatabaseColumn(original);
        Payload restored = converter.convertToEntityAttribute(stored);

        assertEquals(original, restored);
        assertEquals(1, stored[3]);
        assertTrue(stored.length > 4);
    }

    @Test
    void preservesNullAndUsesFallbackForUnknownFormat() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));

        Payload fallback = converter.convertToEntityAttribute(new byte[]{9, 9, 9, 9});

        assertEquals(new Payload("fallback", -1), fallback);
    }

    @Test
    void storesIndependentPayloadCopies() {
        byte[] first = converter.convertToDatabaseColumn(new Payload("alpha", 7));
        byte[] second = converter.convertToDatabaseColumn(new Payload("alpha", 7));

        first[0] = 8;

        assertNotSame(first, second);
        assertEquals(0, second[0]);
    }

    private static final class TestConverter extends BinaryObjectAttributeConverter<Payload> {
        private TestConverter() {
            super();
        }

        @Override
        protected Class<Payload> getValueType() {
            return Payload.class;
        }

        @Override
        protected Payload fallbackConvert(byte[] dbData) {
            return new Payload("fallback", -1);
        }
    }

    public static final class Payload {
        private String name;
        private int count;

        public Payload() {
        }

        private Payload(String name, int count) {
            this.name = name;
            this.count = count;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Payload payload)) {
                return false;
            }
            return count == payload.count && name.equals(payload.name);
        }

        @Override
        public int hashCode() {
            return 31 * name.hashCode() + count;
        }
    }
}
