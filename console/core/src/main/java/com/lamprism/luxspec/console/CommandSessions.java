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

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Objects;

/**
 * Factories for basic dependency-free command sessions.
 *
 * @author RollW
 */
public final class CommandSessions {
    private CommandSessions() {
    }

    /**
     * Creates a session backed by borrowed streams without interactive prompting.
     *
     * @param input       command input
     * @param output      normal output
     * @param diagnostics diagnostic output
     * @return a non-interactive command session
     */
    public static CommandSession nonInteractive(Reader input,
                                                PrintWriter output,
                                                PrintWriter diagnostics) {
        return new BasicCommandSession(input, output, diagnostics);
    }

    /**
     * Creates a non-interactive session over the current process standard streams.
     *
     * <p>Terminal-aware applications should provide their own session
     * implementation when prompting or terminal control is required.</p>
     *
     * @return a process standard-stream session
     */
    public static CommandSession standard() {
        Charset charset = Charset.defaultCharset();
        return nonInteractive(
                new InputStreamReader(System.in, charset),
                new PrintWriter(System.out, true, charset),
                new PrintWriter(System.err, true, charset)
        );
    }

    private static final class BasicCommandSession implements CommandSession {
        private final Reader input;
        private final PrintWriter output;
        private final PrintWriter diagnostics;

        private BasicCommandSession(Reader input, PrintWriter output, PrintWriter diagnostics) {
            this.input = Objects.requireNonNull(input, "input");
            this.output = Objects.requireNonNull(output, "output");
            this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
        }

        @Override
        public Reader getInput() {
            return input;
        }

        @Override
        public PrintWriter getOutput() {
            return output;
        }

        @Override
        public PrintWriter getDiagnostics() {
            return diagnostics;
        }

        @Override
        public <T> PromptResult<T> prompt(PromptSpec<T> prompt) {
            Objects.requireNonNull(prompt, "prompt");
            return PromptResult.unavailable();
        }
    }
}
