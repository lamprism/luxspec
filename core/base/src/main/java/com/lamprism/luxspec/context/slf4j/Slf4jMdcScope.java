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

package com.lamprism.luxspec.context.slf4j;

import com.lamprism.luxspec.context.CorrelationId;
import com.lamprism.luxspec.context.ExecutionContext;
import com.lamprism.luxspec.context.ExecutionContextKeys;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Projects the shared correlation identifier into SLF4J MDC for one lexical scope.
 *
 * <p>MDC is a provider-owned thread-bound projection. This adapter only activates when a caller
 * explicitly opens it and never makes MDC part of the provider-independent context carrier.</p>
 *
 * <p>A scope must close on its owner thread and in LIFO order.</p>
 *
 * @author RollW
 */
public final class Slf4jMdcScope implements AutoCloseable {
    /**
     * The standard MDC key used for the Luxspec correlation identifier.
     */
    public static final String CORRELATION_ID_KEY = "correlationId";

    private static final ThreadLocal<Deque<Slf4jMdcScope>> SCOPES = ThreadLocal.withInitial(ArrayDeque::new);

    private final Thread owner;
    private final @Nullable String previousCorrelationId;
    private boolean closed;

    private Slf4jMdcScope(ExecutionContext context) {
        owner = Thread.currentThread();
        previousCorrelationId = MDC.get(CORRELATION_ID_KEY);
        CorrelationId correlationId = context.get(ExecutionContextKeys.CORRELATION_ID).orElse(null);
        if (correlationId == null) {
            MDC.remove(CORRELATION_ID_KEY);
        } else {
            MDC.put(CORRELATION_ID_KEY, correlationId.value());
        }
        SCOPES.get().push(this);
    }

    /**
     * Opens an MDC projection from an explicit immutable context.
     *
     * @param context the context to project
     * @return the MDC projection scope
     */
    public static Slf4jMdcScope open(ExecutionContext context) {
        return new Slf4jMdcScope(Objects.requireNonNull(context, "context"));
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        if (Thread.currentThread() != owner) {
            throw new IllegalStateException("MDC scope must close on its owner thread");
        }
        Deque<Slf4jMdcScope> scopes = SCOPES.get();
        if (scopes.peek() != this) {
            throw new IllegalStateException("MDC scopes must close in LIFO order");
        }
        if (previousCorrelationId == null) {
            MDC.remove(CORRELATION_ID_KEY);
        } else {
            MDC.put(CORRELATION_ID_KEY, previousCorrelationId);
        }
        scopes.pop();
        if (scopes.isEmpty()) {
            SCOPES.remove();
        }
        closed = true;
    }
}
