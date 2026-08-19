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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void replacesAWorkerContextWhenTheSubmittingThreadHasNoContext() throws Exception {
        ContextKey<String> key = ContextKey.of("request-id", String.class);
        ExecutorService delegate = Executors.newSingleThreadExecutor();
        ContextPropagatingExecutor executor = new ContextPropagatingExecutor(delegate);
        AtomicReference<ExecutionContexts.Scope> workerScope = new AtomicReference<>();
        AtomicReference<Boolean> rootContextPresent = new AtomicReference<>();
        AtomicReference<Boolean> inheritedValue = new AtomicReference<>();

        try {
            delegate.submit(() -> workerScope.set(ExecutionContexts.open(
                    ExecutionContext.empty().with(key, "stale-request")
            ))).get(5L, TimeUnit.SECONDS);

            executor.execute(() -> {
                ExecutionContext context = ExecutionContexts.requireCurrent();
                rootContextPresent.set(true);
                inheritedValue.set(context.get(key).isPresent());
            });
            delegate.submit(() -> {
            }).get(5L, TimeUnit.SECONDS);

            delegate.submit(() -> workerScope.get().close()).get(5L, TimeUnit.SECONDS);
            boolean workerIsClean = delegate.submit(() -> ExecutionContexts.current().isEmpty())
                    .get(5L, TimeUnit.SECONDS);

            assertEquals(true, rootContextPresent.get());
            assertEquals(false, inheritedValue.get());
            assertTrue(workerIsClean);
        } finally {
            delegate.shutdownNow();
        }
    }
}
