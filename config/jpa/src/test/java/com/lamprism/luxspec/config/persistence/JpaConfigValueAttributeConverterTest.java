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

package com.lamprism.luxspec.config.persistence;

import com.lamprism.luxspec.config.source.RawConfigValue;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JpaConfigValueAttributeConverterTest {
    private final JpaConfigValueAttributeConverter converter = new JpaConfigValueAttributeConverter();

    @Test
    void roundTripsScalarKindsAndNestedListBoundaries() {
        RawConfigValue value = RawConfigValue.list(List.of(
                RawConfigValue.integer(1000),
                RawConfigValue.string("1KB"),
                RawConfigValue.decimal(new BigDecimal("1.250")),
                RawConfigValue.list(List.of(RawConfigValue.booleanValue(true)))
        ));

        String payload = converter.convertToDatabaseColumn(value);

        assertEquals(value, converter.convertToEntityAttribute(payload));
    }

    @Test
    void preservesNullAndStableScalarPayloadShape() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
        assertEquals(
                "{\"kind\":\"DECIMAL\",\"value\":\"1.250\"}",
                converter.convertToDatabaseColumn(RawConfigValue.decimal(new BigDecimal("1.250")))
        );
    }

    @Test
    void returnsNullForMalformedDatabasePayloads() {
        assertNull(converter.convertToEntityAttribute("null"));
        assertNull(
                converter.convertToEntityAttribute("{\"kind\":\"BOOLEAN\",\"value\":\"yes\"}")
        );
        assertNull(
                converter.convertToEntityAttribute("{\"kind\":\"UNKNOWN\",\"value\":\"value\"}")
        );
        assertNull(converter.convertToEntityAttribute("{\"kind\":\"LIST\"}"));
        assertNull(
                converter.convertToEntityAttribute("{\"kind\":\"STRING\",\"value\":\"value\"} trailing")
        );
    }

}
