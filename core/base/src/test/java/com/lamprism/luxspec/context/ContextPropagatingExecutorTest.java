package com.lamprism.luxspec.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ContextPropagatingExecutorTest {
    @Test
    void propagatesTheCapturedContextAndCleansUpTheWorkerThread() throws Exception {
        ContextKey<String> key = ContextKey.of("request-id", String.class);
        ExecutorService delegate = Executors.newSingleThreadExecutor();
        ContextPropagatingExecutor executor = new ContextPropagatingExecutor(delegate);
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<String> propagated = new AtomicReference<>();
        AtomicReference<Boolean> workerCleaned = new AtomicReference<>();

        try (ExecutionContexts.Scope ignored = ExecutionContexts.open(ExecutionContext.empty().with(key, "request-42"))) {
            executor.execute(() -> {
                propagated.set(ExecutionContexts.requireCurrent().get(key).orElseThrow());
                completed.countDown();
            });
        }
        assertTrue(completed.await(5L, TimeUnit.SECONDS));
        delegate.submit(() -> workerCleaned.set(ExecutionContexts.current().isEmpty())).get(5L, TimeUnit.SECONDS);
        delegate.shutdownNow();

        assertEquals("request-42", propagated.get());
        assertEquals(true, workerCleaned.get());
    }

    @Test
    void executesDetachedWorkWithANewEmptyRootContext() throws Exception {
        ContextKey<String> key = ContextKey.of("request-id", String.class);
        ExecutorService delegate = Executors.newSingleThreadExecutor();
        ContextPropagatingExecutor executor = new ContextPropagatingExecutor(delegate);
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<Boolean> inheritedValue = new AtomicReference<>();
        AtomicReference<Boolean> rootContextPresent = new AtomicReference<>();

        try (ExecutionContexts.Scope ignored = ExecutionContexts.open(ExecutionContext.empty().with(key, "request-42"))) {
            executor.executeDetached(() -> {
                ExecutionContext context = ExecutionContexts.requireCurrent();
                inheritedValue.set(context.get(key).isPresent());
                rootContextPresent.set(ExecutionContexts.current().isPresent());
                completed.countDown();
            });
        }
        assertTrue(completed.await(5L, TimeUnit.SECONDS));
        delegate.shutdownNow();

        assertEquals(false, inheritedValue.get());
        assertEquals(true, rootContextPresent.get());
    }
}
