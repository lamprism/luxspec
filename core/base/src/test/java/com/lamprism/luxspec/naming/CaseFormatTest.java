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

package com.lamprism.luxspec.naming;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CaseFormatTest {
    @Test
    void convertsBetweenCommonCaseFormats() {
        assertEquals(
                "displayName",
                CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, "display_name")
        );
        assertEquals(
                "displayName",
                CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, "display-name")
        );
        assertEquals(
                "DisplayName",
                CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, "display_name")
        );
        assertEquals(
                "DISPLAY_NAME",
                CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, "displayName")
        );
    }

    @Test
    void createsComposableConverters() {
        NameConverter converter = CaseFormat.LOWER_UNDERSCORE
                .to(CaseFormat.LOWER_CAMEL)
                .andThen(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE));

        assertEquals("DISPLAY_NAME", converter.convert("display_name"));
    }

    @Test
    void rejectsEmptySeparatedSegments() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, "display__name")
        );
    }
}
