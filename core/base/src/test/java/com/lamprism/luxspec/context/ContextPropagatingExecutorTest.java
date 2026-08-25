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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContextPropagatingExecutorTest {
    @Test
    void propagatesTheCapturedContextThroughSelectedStorage() throws Exception {
        ContextKey<String> key = ContextKey.of("request-id", String.class);
        ExecutionContextStorage storage = new ThreadLocalExecutionContextStorage();
        ExecutorService delegate = Executors.newSingleThreadExecutor();
        ContextPropagatingExecutor executor = new ContextPropagatingExecutor(delegate, storage);
        AtomicReference<String> propagated = new AtomicReference<>();

        try (ExecutionContextStorage.Scope ignored = storage.open(
                ExecutionContext.empty().with(key, "request-42")
        )) {
            executor.execute(() -> propagated.set(storage.requireCurrent().get(key).orElseThrow()));
        }
        delegate.shutdown();
        delegate.awaitTermination(5L, TimeUnit.SECONDS);

        assertEquals("request-42", propagated.get());
    }

    @Test
    void acceptsAnExplicitContextThroughSelectedStorage() throws Exception {
        ContextKey<String> key = ContextKey.of("request-id", String.class);
        ExecutionContextStorage storage = new ThreadLocalExecutionContextStorage();
        ExecutorService delegate = Executors.newSingleThreadExecutor();
        ContextPropagatingExecutor executor = new ContextPropagatingExecutor(delegate, storage);
        AtomicReference<String> propagated = new AtomicReference<>();
        ExecutionContext context = ExecutionContext.empty().with(key, "request-42");

        executor.execute(context, () -> propagated.set(storage.requireCurrent().get(key).orElseThrow()));
        delegate.shutdown();
        delegate.awaitTermination(5L, TimeUnit.SECONDS);

        assertEquals("request-42", propagated.get());
    }
}
