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

package com.lamprism.luxspec.observability.observation;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimpleObservationLinkTest {
    @Test
    void copiesAttributesAndAllowsReadableValues() {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("reason", "retry after timeout");
        ObservationLink link = ObservationLink.of("trace-1", "span-1", attributes);
        attributes.put("ignored", "later value");

        assertEquals("trace-1", link.traceId());
        assertEquals("span-1", link.spanId());
        assertEquals(Map.of("reason", "retry after timeout"), link.attributes());
        assertThrows(UnsupportedOperationException.class, () -> link.attributes().put("new", "value"));
    }

    @Test
    void rejectsUnsafeAttributeNames() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ObservationLink.of("trace-1", "span-1", Map.of("invalid name", "value"))
        );
    }
}
