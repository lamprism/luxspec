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
import com.lamprism.luxspec.context.ExecutionContextStorage;
import com.lamprism.luxspec.context.Slf4jMdcScope;
import org.springframework.core.task.TaskDecorator;

import java.util.Objects;

/**
 * Installs a context snapshot through an explicitly selected Spring task boundary.
 *
 * <p>The application supplies the storage implementation. This decorator does not select a
 * storage strategy.</p>
 *
 * @author RollW
 */
public final class SpringExecutionContextTaskDecorator implements TaskDecorator {
    private final ExecutionContextStorage storage;

    /**
     * Creates a decorator using one explicitly selected context storage.
     *
     * @param storage the context storage used for capture and installation
     */
    public SpringExecutionContextTaskDecorator(ExecutionContextStorage storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    @Override
    public Runnable decorate(Runnable runnable) {
        Runnable nonNullRunnable = Objects.requireNonNull(runnable, "runnable");
        ExecutionContext capturedContext = storage.current().orElseGet(ExecutionContext::empty);
        return () -> {
            try (ExecutionContextStorage.Scope ignored = storage.open(capturedContext);
                 Slf4jMdcScope mdcScope = Slf4jMdcScope.open(capturedContext)) {
                nonNullRunnable.run();
            }
        };
    }
}
