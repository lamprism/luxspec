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

/**
 * Stable categories for failures produced by command processing.
 *
 * @author RollW
 */
public enum CommandFailureKind {
    /**
     * The token sequence or command definition is invalid.
     */
    USAGE(2),
    /**
     * A command handler failed while executing.
     */
    EXECUTION(1),
    /**
     * The requested user interaction is unavailable or was rejected.
     */
    INTERACTION(1),
    /**
     * The command framework encountered an unexpected internal failure.
     */
    INTERNAL(1);

    private final int defaultExitCode;

    CommandFailureKind(int defaultExitCode) {
        this.defaultExitCode = defaultExitCode;
    }

    /**
     * @return the default process exit code for this failure kind
     */
    public int getDefaultExitCode() {
        return defaultExitCode;
    }
}
