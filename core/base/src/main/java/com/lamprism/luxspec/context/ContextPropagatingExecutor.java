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

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * Captures the complete active ExecutionContext when work is submitted to another Executor.
 *
 * @author RollW
 */
public final class ContextPropagatingExecutor implements Executor {
    private final Executor delegate;

    /**
     * Creates an executor wrapper that propagates the submitting thread's active context.
     *
     * @param delegate the executor that runs submitted work
     */
    public ContextPropagatingExecutor(Executor delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public void execute(Runnable command) {
        Runnable nonNullCommand = Objects.requireNonNull(command, "command");
        Optional<ExecutionContext> captured = ExecutionContexts.snapshot();
        delegate.execute(() -> run(captured, nonNullCommand));
    }

    /**
     * Executes work inside a new empty root context instead of inheriting the submitting context.
     *
     * @param command the detached work to run
     */
    public void executeDetached(Runnable command) {
        Runnable nonNullCommand = Objects.requireNonNull(command, "command");
        delegate.execute(() -> run(ExecutionContext.empty(), nonNullCommand));
    }

    private void run(Optional<ExecutionContext> captured, Runnable command) {
        if (captured.isEmpty()) {
            command.run();
            return;
        }
        run(captured.orElseThrow(), command);
    }

    private void run(ExecutionContext context, Runnable command) {
        try (ExecutionContexts.Scope ignored = ExecutionContexts.open(context)) {
            command.run();
        }
    }
}
