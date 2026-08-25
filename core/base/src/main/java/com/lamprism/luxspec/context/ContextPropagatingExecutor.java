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
import java.util.concurrent.Executor;

/**
 * Installs an explicitly selected context while an executor task runs.
 *
 * <p>The executor does not own a global context. It uses the supplied
 * {@link ExecutionContextStorage} for capture and task installation.</p>
 *
 * @author RollW
 */
public final class ContextPropagatingExecutor implements Executor {
    private final Executor delegate;
    private final ExecutionContextStorage storage;

    /**
     * Creates an executor backed by an explicitly selected context storage.
     *
     * @param delegate the executor that runs submitted work
     * @param storage  the context storage used for capture and installation
     */
    public ContextPropagatingExecutor(Executor delegate, ExecutionContextStorage storage) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    /**
     * Executes work with the supplied immutable context.
     *
     * @param context the context to install for the task
     * @param command the task to run
     */
    public void execute(ExecutionContext context, Runnable command) {
        ExecutionContext nonNullContext = Objects.requireNonNull(context, "context");
        Runnable nonNullCommand = Objects.requireNonNull(command, "command");
        delegate.execute(() -> run(nonNullContext, nonNullCommand));
    }

    /**
     * Executes work using a snapshot from the explicitly selected context facade.
     *
     * @param command the task to run
     */
    @Override
    public void execute(Runnable command) {
        ExecutionContext captured = storage.current().orElseGet(ExecutionContext::empty);
        execute(captured, command);
    }

    private void run(ExecutionContext context, Runnable command) {
        try (ExecutionContextStorage.Scope ignored = storage.open(context);
             Slf4jMdcScope mdcScope = Slf4jMdcScope.open(context)) {
            command.run();
        }
    }
}
