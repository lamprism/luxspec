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

import org.slf4j.MDC;

import java.util.Objects;
import java.util.Optional;

/**
 * Projects safe shared execution values into SLF4J MDC for one lexical scope.
 *
 * @author RollW
 */
public final class Slf4jMdcScope implements AutoCloseable {
    public static final String CORRELATION_ID_KEY = "correlationId";

    private final String previousCorrelationId;
    private boolean closed;

    private Slf4jMdcScope(Optional<ExecutionContext> context) {
        previousCorrelationId = MDC.get(CORRELATION_ID_KEY);
        CorrelationId correlationId = context.flatMap(value -> value.get(ExecutionContextKeys.CORRELATION_ID))
                .orElse(null);
        if (correlationId == null) {
            MDC.remove(CORRELATION_ID_KEY);
        } else {
            MDC.put(CORRELATION_ID_KEY, correlationId.value());
        }
    }

    /**
     * Opens an MDC projection from the current execution context.
     */
    public static Slf4jMdcScope open() {
        return new Slf4jMdcScope(ExecutionContexts.current());
    }

    /**
     * Opens an MDC projection from an explicit immutable execution context.
     */
    public static Slf4jMdcScope open(ExecutionContext context) {
        return new Slf4jMdcScope(Optional.of(Objects.requireNonNull(context, "context")));
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        if (previousCorrelationId == null) {
            MDC.remove(CORRELATION_ID_KEY);
        } else {
            MDC.put(CORRELATION_ID_KEY, previousCorrelationId);
        }
        closed = true;
    }
}
