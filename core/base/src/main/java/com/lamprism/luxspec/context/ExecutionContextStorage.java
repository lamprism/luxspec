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

import java.util.Optional;

/**
 * Stores an explicit execution-context value at one application boundary.
 *
 * <p>The context carrier remains independent from this storage role. Implementations may use a
 * request attribute, a framework scope, an explicit invocation object, or an opt-in thread-bound
 * strategy.</p>
 *
 * @author RollW
 */
public interface ExecutionContextStorage {
    /**
     * Returns the context currently visible through this storage boundary.
     *
     * @return the current context when one is available
     */
    Optional<ExecutionContext> current();

    /**
     * Opens a nested context for the storage boundary.
     *
     * @param context the context to expose
     * @return the scope that restores the prior storage state
     */
    Scope open(ExecutionContext context);

    /**
     * Returns the current context or fails when no context is available.
     *
     * @return the current context
     * @throws MissingExecutionContextException when no context is available
     */
    default ExecutionContext requireCurrent() {
        return current().orElseThrow(MissingExecutionContextException::new);
    }

    /**
     * A storage scope that restores the previous value when closed.
     */
    interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
