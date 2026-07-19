package com.lamprism.luxspec.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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
