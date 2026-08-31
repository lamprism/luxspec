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

import java.io.PrintWriter;
import java.io.Reader;

/**
 * Provider-neutral runtime surface shared by process CLI and application Shell.
 *
 * <p>A process invocation normally receives a short-lived session. An
 * application Shell can reuse one session for multiple command invocations.
 * The session borrows its streams and never closes them.</p>
 *
 * @author RollW
 */
public interface CommandSession {
    /**
     * @return command data input
     */
    Reader getInput();

    /**
     * @return normal command output
     */
    PrintWriter getOutput();

    /**
     * @return diagnostic output corresponding to stderr
     */
    PrintWriter getDiagnostics();

    /**
     * Requests one typed user interaction through the current provider.
     *
     * @param prompt interaction specification
     * @param <T>    requested value type
     * @return the accepted value or an explicit non-interactive outcome
     */
    <T> PromptResult<T> prompt(PromptSpec<T> prompt);
}
