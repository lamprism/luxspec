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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryResultTest {
    @Test
    void permitsAnEmptyPageBeyondTheExactTotal() {
        assertDoesNotThrow(() -> new PageResult<>(List.of(), 100L, 10, 3L));
    }

    @Test
    void rejectsResultItemsBeyondTheRequestedWindowLimit() {
        assertThrows(IllegalArgumentException.class, () -> new PageResult<>(List.of("first", "second"), 0L, 1, 2L));
        assertThrows(IllegalArgumentException.class, () -> new SliceResult<>(List.of("first", "second"), 0L, 1, true));
    }
}
