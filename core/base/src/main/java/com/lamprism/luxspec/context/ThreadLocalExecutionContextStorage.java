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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;

/**
 * Optional thread-local execution-context storage.
 *
 * <p>This implementation is never selected by the core API implicitly. Applications must create
 * and inject it when a thread-bound context is appropriate.</p>
 *
 * @author RollW
 */
public final class ThreadLocalExecutionContextStorage implements ExecutionContextStorage {
    private final ThreadLocal<Deque<ExecutionContext>> stacks = ThreadLocal.withInitial(ArrayDeque::new);

    @Override
    public Optional<ExecutionContext> current() {
        return Optional.ofNullable(stacks.get().peek());
    }

    @Override
    public Scope open(ExecutionContext context) {
        ExecutionContext nonNullContext = Objects.requireNonNull(context, "context");
        Deque<ExecutionContext> stack = stacks.get();
        stack.push(nonNullContext);
        return new Scope(Thread.currentThread(), nonNullContext);
    }

    private final class Scope implements ExecutionContextStorage.Scope {
        private final Thread owner;
        private final ExecutionContext context;
        private boolean closed;

        private Scope(Thread owner, ExecutionContext context) {
            this.owner = owner;
            this.context = context;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            if (Thread.currentThread() != owner) {
                throw new IllegalStateException("ExecutionContext scope must close on its owner thread");
            }
            Deque<ExecutionContext> stack = stacks.get();
            if (stack.peek() != context) {
                throw new IllegalStateException("ExecutionContext scopes must close in LIFO order");
            }
            stack.pop();
            if (stack.isEmpty()) {
                stacks.remove();
            }
            closed = true;
        }
    }
}
