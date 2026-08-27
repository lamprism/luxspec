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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigKeyTest {
    @Test
    void usesOneModelForFixedAndParameterizedKeys() {
        ConfigKey fixed = ConfigKey.of("application.timeout");
        ConfigKey expression = ConfigKey.template(
                "service.{tenant}.timeout",
                List.of(new ConfigParameter("tenant", Set.of("north")))
        );

        assertFalse(fixed.isParameterized());
        assertTrue(expression.isParameterized());
        assertEquals(List.of("application", "timeout"), fixed.getSegments());
        assertEquals(List.of("service", "{tenant}", "timeout"), expression.getSegments());
    }

    @Test
    void bindsAndMatchesThroughTheUnifiedKeyType() {
        ConfigKey expression = ConfigKey.template(
                "service.{tenant}.timeout",
                List.of(new ConfigParameter("tenant", Set.of("north")))
        );

        ConfigKey complete = expression.bind(Map.of("tenant", "north"));

        assertFalse(complete.isParameterized());
        assertEquals("service.north.timeout", complete.getValue());
        assertEquals(Map.of("tenant", "north"), expression.match(complete));
    }

    @Test
    void usesParameterSchemaInTemplateEquality() {
        ConfigParameter north = new ConfigParameter("tenant", Set.of("north"));
        ConfigParameter northCopy = new ConfigParameter("tenant", Set.of("north"));
        ConfigParameter south = new ConfigParameter("tenant", Set.of("south"));
        ConfigKey northTemplate = ConfigKey.template(
                "service.{tenant}.timeout",
                List.of(north)
        );
        ConfigKey northTemplateCopy = ConfigKey.template(
                "service.{tenant}.timeout",
                List.of(northCopy)
        );
        ConfigKey southTemplate = ConfigKey.template(
                "service.{tenant}.timeout",
                List.of(south)
        );

        assertEquals(north, northCopy);
        assertEquals(north.hashCode(), northCopy.hashCode());
        assertNotEquals(north, south);
        assertEquals(northTemplate, northTemplateCopy);
        assertEquals(northTemplate.hashCode(), northTemplateCopy.hashCode());
        assertNotEquals(northTemplate, southTemplate);
    }
}
