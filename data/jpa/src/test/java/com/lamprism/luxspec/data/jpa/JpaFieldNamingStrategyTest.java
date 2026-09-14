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

package com.lamprism.luxspec.data.jpa;

import com.lamprism.luxspec.naming.CaseFormat;
import com.lamprism.luxspec.naming.NameConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JpaFieldNamingStrategyTest {
    @Test
    void convertsSupportedNamingStyles() {
        assertEquals("DisplayName", JpaFieldNamingStrategy.identity().toAttributeName("DisplayName"));
        assertEquals(
                "displayName",
                JpaFieldNamingStrategy.from(
                        CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL)
                ).toAttributeName("display_name")
        );
        assertEquals(
                "displayName",
                JpaFieldNamingStrategy.from(NameConverter.identity()).toAttributeName("displayName")
        );
    }

    @Test
    void rejectsEmptySegmentsWhenASeparatorStrategyIsSelected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> JpaFieldNamingStrategy.from(
                        CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL)
                ).toAttributeName("display__name")
        );
    }
}
