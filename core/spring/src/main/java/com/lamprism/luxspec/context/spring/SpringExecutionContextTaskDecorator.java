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

import com.lamprism.luxspec.context.ExecutionContext;
import com.lamprism.luxspec.context.ExecutionContexts;
import org.springframework.core.task.TaskDecorator;

import java.util.Objects;

/**
 * Propagates the complete Luxspec execution context through a Spring task executor.
 *
 * <p>The snapshot is captured when Spring decorates the task and the scope is closed after the
 * task finishes, including when the task throws.</p>
 *
 * @author RollW
 */
public class SpringExecutionContextTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
        Runnable nonNullRunnable = Objects.requireNonNull(runnable, "runnable");
        ExecutionContext capturedContext = ExecutionContexts.snapshot().orElseGet(ExecutionContext::empty);
        return () -> run(capturedContext, nonNullRunnable);
    }

    private static void run(ExecutionContext capturedContext, Runnable runnable) {
        try (ExecutionContexts.Scope ignored = ExecutionContexts.open(capturedContext)) {
            runnable.run();
        }
    }
}
