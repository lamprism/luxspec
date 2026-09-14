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

package com.lamprism.luxspec.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CacheNameTest {
    @Test
    void acceptsOpaqueApplicationIdentifiers() {
        assertEquals("tenant/cache:v2", CacheName.of("tenant/cache:v2").getValue());
        assertEquals("tenant cache", CacheName.of("tenant cache").getValue());
        assertEquals("\u7f13\u5b58", CacheName.of("\u7f13\u5b58").getValue());
    }

    @Test
    void rejectsBlankIdentifiers() {
        assertThrows(IllegalArgumentException.class, () -> CacheName.of(""));
        assertThrows(IllegalArgumentException.class, () -> CacheName.of(" \t"));
    }
}
