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

package com.lamprism.luxspec.context.spring;

import com.lamprism.luxspec.context.ContextKey;
import com.lamprism.luxspec.context.ExecutionContext;
import com.lamprism.luxspec.context.ExecutionContexts;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringExecutionContextTaskDecoratorTest {
    @Test
    void propagatesTheCapturedContextAndRestoresTheWorkerThread() throws Exception {
        ContextKey<String> key = ContextKey.of("request-id", String.class);
        SpringExecutionContextTaskDecorator decorator = new SpringExecutionContextTaskDecorator();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        AtomicReference<String> observedValue = new AtomicReference<>();

        try {
            try (ExecutionContexts.Scope ignored = ExecutionContexts.open(
                    ExecutionContext.empty().with(key, "request-42")
            )) {
                Runnable decorated = decorator.decorate(() -> observedValue.set(
                        ExecutionContexts.requireCurrent().get(key).orElseThrow()
                ));
                executor.submit(decorated).get(5L, TimeUnit.SECONDS);
            }

            boolean workerIsClean = executor.submit(() -> ExecutionContexts.current().isEmpty())
                    .get(5L, TimeUnit.SECONDS);
            assertEquals("request-42", observedValue.get());
            assertTrue(workerIsClean);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void installsAnEmptyContextWhenNoContextWasCaptured() throws Exception {
        SpringExecutionContextTaskDecorator decorator = new SpringExecutionContextTaskDecorator();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        AtomicReference<Boolean> emptyRootPresent = new AtomicReference<>();

        try {
            Runnable decorated = decorator.decorate(() -> {
                emptyRootPresent.set(ExecutionContexts.current().isPresent());
            });
            boolean contextIsAbsent = executor.submit(() -> {
                decorated.run();
                return ExecutionContexts.current().isEmpty();
            }).get(5L, TimeUnit.SECONDS);
            assertTrue(emptyRootPresent.get());
            assertTrue(contextIsAbsent);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void replacesAWorkerContextWhenNoContextWasCaptured() throws Exception {
        ContextKey<String> key = ContextKey.of("request-id", String.class);
        SpringExecutionContextTaskDecorator decorator = new SpringExecutionContextTaskDecorator();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        AtomicReference<ExecutionContexts.Scope> workerScope = new AtomicReference<>();
        AtomicReference<Boolean> inheritedValue = new AtomicReference<>();

        try {
            executor.submit(() -> workerScope.set(ExecutionContexts.open(
                    ExecutionContext.empty().with(key, "stale-request")
            ))).get(5L, TimeUnit.SECONDS);

            Runnable decorated = decorator.decorate(() -> inheritedValue.set(
                    ExecutionContexts.requireCurrent().get(key).isPresent()
            ));
            executor.submit(decorated).get(5L, TimeUnit.SECONDS);

            executor.submit(() -> workerScope.get().close()).get(5L, TimeUnit.SECONDS);
            boolean workerIsClean = executor.submit(() -> ExecutionContexts.current().isEmpty())
                    .get(5L, TimeUnit.SECONDS);

            assertEquals(false, inheritedValue.get());
            assertTrue(workerIsClean);
        } finally {
            executor.shutdownNow();
        }
    }
}
