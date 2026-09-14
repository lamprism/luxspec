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

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Immutable result of one command application execution.
 *
 * @author RollW
 */
public final class CommandResult {
    private final int exitCode;
    private final @Nullable CommandFailure failure;

    private CommandResult(int exitCode, @Nullable CommandFailure failure) {
        if (exitCode < 0) {
            throw new IllegalArgumentException("Exit code cannot be negative: " + exitCode);
        }
        if (failure != null && exitCode == 0) {
            throw new IllegalArgumentException("A failure must have a nonzero exit code");
        }
        this.exitCode = exitCode;
        this.failure = failure;
    }

    /**
     * @return a successful result
     */
    public static CommandResult success() {
        return new CommandResult(0, null);
    }

    /**
     * Creates a result with an explicit exit code and no framework failure.
     *
     * @param exitCode nonnegative exit code
     * @return the command result
     */
    public static CommandResult exitCode(int exitCode) {
        return new CommandResult(exitCode, null);
    }

    /**
     * Creates a result containing a structured failure.
     *
     * @param failure structured command failure
     * @return a failed command result
     */
    public static CommandResult failure(CommandFailure failure) {
        CommandFailure nonNullFailure = Objects.requireNonNull(failure, "failure");
        return new CommandResult(nonNullFailure.getExitCode(), nonNullFailure);
    }

    /**
     * @return the explicit exit code
     */
    public int getExitCode() {
        return exitCode;
    }

    /**
     * @return the failure, or {@code null} for a normal result
     */
    public @Nullable CommandFailure getFailure() {
        return failure;
    }

    /**
     * @return whether this result completed successfully
     */
    public boolean isSuccess() {
        return exitCode == 0 && failure == null;
    }
}
