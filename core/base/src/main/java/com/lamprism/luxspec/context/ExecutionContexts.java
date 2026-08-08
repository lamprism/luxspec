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
 * Manages the thread-owned stack of active immutable ExecutionContexts.
 *
 * @author RollW
 */
public final class ExecutionContexts {
    private static final ThreadLocal<Deque<ExecutionContext>> STACKS = ThreadLocal.withInitial(ArrayDeque::new);

    private ExecutionContexts() {
    }

    /**
     * Returns the active immutable context for the current thread.
     *
     * @return the active context, when one exists
     */
    public static Optional<ExecutionContext> current() {
        return Optional.ofNullable(STACKS.get().peek());
    }

    /**
     * Captures the active immutable context for explicit asynchronous propagation.
     *
     * @return the active context snapshot, when one exists
     */
    public static Optional<ExecutionContext> snapshot() {
        return current();
    }

    /**
     * Returns the active context or fails when no scope is open.
     *
     * @return the active context
     * @throws MissingExecutionContextException when no scope is open
     */
    public static ExecutionContext requireCurrent() {
        return current().orElseThrow(MissingExecutionContextException::new);
    }

    /**
     * Opens a thread-owned scope for one immutable context.
     *
     * @param context the context to make active
     * @return the scope that restores the prior context when closed
     */
    public static Scope open(ExecutionContext context) {
        Deque<ExecutionContext> stack = STACKS.get();
        ExecutionContext nonNullContext = Objects.requireNonNull(context, "context");
        stack.push(nonNullContext);
        return new Scope(Thread.currentThread(), nonNullContext);
    }

    /**
     * Restores the previous context when closed in stack order.
     *
     * @author RollW
     */
    public static final class Scope implements AutoCloseable {
        private final Thread owner;
        private final ExecutionContext context;
        private boolean closed;

        private Scope(Thread owner, ExecutionContext context) {
            this.owner = owner;
            this.context = context;
        }

        /**
         * Restores the previous context when called once by the owner thread in LIFO order.
         */
        @Override
        public void close() {
            if (closed) {
                return;
            }
            if (Thread.currentThread() != owner) {
                throw new IllegalStateException("ExecutionContext scope must close on its owner thread");
            }
            Deque<ExecutionContext> stack = STACKS.get();
            if (stack.peek() != context) {
                throw new IllegalStateException("ExecutionContext scopes must close in LIFO order");
            }
            stack.pop();
            if (stack.isEmpty()) {
                STACKS.remove();
            }
            closed = true;
        }
    }
}
