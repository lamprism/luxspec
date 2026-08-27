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

package com.lamprism.luxspec.context;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutionContextTest {
    @Test
    void derivesImmutableTypedValues() {
        ContextKey<String> key = ContextKey.of("request-id", String.class);
        ExecutionContext empty = ExecutionContext.empty();
        ExecutionContext context = empty.with(key, "request-42");

        assertEquals("request-42", context.get(key).orElseThrow());
        assertEquals("request-43", context.replace(key, "request-43").get(key).orElseThrow());
        assertEquals("request-42", context.get(key).orElseThrow());
        assertTrue(context.without(key).get(key).isEmpty());
    }

    @Test
    void rejectsDuplicateAndMissingKeyUpdates() {
        ContextKey<String> key = ContextKey.of("request-id", String.class);
        ExecutionContext context = ExecutionContext.empty().with(key, "request-42");

        assertThrows(IllegalStateException.class, () -> context.with(key, "request-43"));
        assertThrows(IllegalStateException.class, () -> ExecutionContext.empty().replace(key, "request-43"));
    }

    @Test
    void treatsKeysAsReferenceIdentities() {
        ContextKey<String> registeredKey = ContextKey.of("request-id", String.class);
        ContextKey<String> equivalentDeclaration = ContextKey.of("request-id", String.class);
        ExecutionContext context = ExecutionContext.empty().with(registeredKey, "request-42");

        assertTrue(context.get(equivalentDeclaration).isEmpty());
    }
}
