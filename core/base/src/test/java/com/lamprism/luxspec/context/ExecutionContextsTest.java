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

class ExecutionContextsTest {
    @Test
    void hasNoCurrentContextOutsideAnOpenScope() {
        assertTrue(ExecutionContexts.current().isEmpty());
        assertThrows(MissingExecutionContextException.class, ExecutionContexts::requireCurrent);
    }

    @Test
    void restoresNestedContextsAndCleansUpAfterAnException() {
        ContextKey<String> key = ContextKey.of("request-id", String.class);
        ExecutionContext outerContext = ExecutionContext.empty().with(key, "outer");
        ExecutionContext innerContext = ExecutionContext.empty().with(key, "inner");

        try (ExecutionContexts.Scope ignored = ExecutionContexts.open(outerContext)) {
            assertSame(outerContext, ExecutionContexts.requireCurrent());
            try (ExecutionContexts.Scope nested = ExecutionContexts.open(innerContext)) {
                assertSame(innerContext, ExecutionContexts.requireCurrent());
            }
            assertSame(outerContext, ExecutionContexts.requireCurrent());
        }

        assertThrows(IllegalStateException.class, () -> {
            try (ExecutionContexts.Scope ignored = ExecutionContexts.open(outerContext)) {
                throw new IllegalStateException("expected failure");
            }
        });
        assertTrue(ExecutionContexts.current().isEmpty());
    }

    @Test
    void capturesAnImmutableCurrentContextSnapshot() {
        ContextKey<String> key = ContextKey.of("request-id", String.class);
        ExecutionContext context = ExecutionContext.empty().with(key, "request-42");
        ExecutionContext snapshot;

        try (ExecutionContexts.Scope ignored = ExecutionContexts.open(context)) {
            snapshot = ExecutionContexts.snapshot().orElseThrow();
        }

        assertEquals("request-42", snapshot.get(key).orElseThrow());
        assertTrue(ExecutionContexts.current().isEmpty());
    }
}
