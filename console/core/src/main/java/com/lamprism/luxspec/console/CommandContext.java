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

package com.lamprism.luxspec.console;

import java.util.Objects;

/**
 * Immutable per-invocation context supplied to a command handler.
 *
 * @author RollW
 */
public final class CommandContext {
    private final CommandInvocation invocation;
    private final CommandSession session;

    /**
     * Creates an invocation context.
     *
     * @param invocation parsed command invocation
     * @param session    command execution session
     */
    public CommandContext(CommandInvocation invocation, CommandSession session) {
        this.invocation = Objects.requireNonNull(invocation, "invocation");
        this.session = Objects.requireNonNull(session, "session");
    }

    /**
     * @return the immutable parsed invocation
     */
    public CommandInvocation getInvocation() {
        return invocation;
    }

    /**
     * @return the runtime session for this invocation
     */
    public CommandSession getSession() {
        return session;
    }
}
