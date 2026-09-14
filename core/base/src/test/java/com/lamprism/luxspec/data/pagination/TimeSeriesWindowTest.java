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

package com.lamprism.luxspec.data.pagination;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeSeriesWindowTest {
    private static final Instant FIRST = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void includesBothRangeBoundaries() {
        TimeSeriesWindow window = new TimeSeriesWindow(FIRST, FIRST.plusSeconds(2L), 10);

        assertTrue(window.contains(FIRST));
        assertTrue(window.contains(FIRST.plusSeconds(2L)));
        assertFalse(window.contains(FIRST.minusSeconds(1L)));
        assertFalse(window.contains(FIRST.plusSeconds(3L)));
    }

    @Test
    void rejectsInvalidBoundsAndPointLimits() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TimeSeriesWindow(FIRST.plusSeconds(1L), FIRST, 10)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new TimeSeriesWindow(FIRST, FIRST, 0)
        );
    }
}
