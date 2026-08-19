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

package com.lamprism.luxspec.user.security.password;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryUserPasswordStoreTest {
    @Test
    void supportsValueBasedCompareAndSetReplacement() {
        InMemoryUserPasswordStore store = new InMemoryUserPasswordStore();
        EncodedPassword initial = new EncodedPassword("initial");
        EncodedPassword replacement = new EncodedPassword("replacement");
        store.put(1L, initial);

        assertTrue(store.replace(1L, new EncodedPassword("initial"), replacement));
        assertEquals(replacement, store.find(1L).orElseThrow());
        assertFalse(store.replace(1L, initial, new EncodedPassword("stale")));
    }
}
