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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutionContextStorageTest {
    @Test
    void selectedStorageRestoresNestedContexts() {
        ContextKey<String> key = ContextKey.of("request-id", String.class);
        ExecutionContextStorage storage = new ThreadLocalExecutionContextStorage();
        ExecutionContext outer = ExecutionContext.empty().with(key, "outer");
        ExecutionContext inner = ExecutionContext.empty().with(key, "inner");

        try (ExecutionContextStorage.Scope ignored = storage.open(outer)) {
            assertSame(outer, storage.requireCurrent());
            try (ExecutionContextStorage.Scope nested = storage.open(inner)) {
                assertSame(inner, storage.requireCurrent());
            }
            assertSame(outer, storage.requireCurrent());
        }

        assertTrue(storage.current().isEmpty());
    }

    @Test
    void rejectsOutOfOrderAndCrossThreadScopeClose() throws Exception {
        ExecutionContextStorage storage = new ThreadLocalExecutionContextStorage();
        ExecutionContextStorage.Scope scope = storage.open(ExecutionContext.empty());
        ExecutionContextStorage.Scope nested = storage.open(ExecutionContext.empty());

        assertThrows(IllegalStateException.class, scope::close);
        nested.close();
        scope.close();

        ExecutionContextStorage.Scope crossThreadScope = storage.open(ExecutionContext.empty());
        try {
            Thread thread = new Thread(() -> assertThrows(
                    IllegalStateException.class,
                    crossThreadScope::close
            ));
            thread.start();
            thread.join();
        } finally {
            crossThreadScope.close();
        }
        assertEquals(true, storage.current().isEmpty());
    }
}
