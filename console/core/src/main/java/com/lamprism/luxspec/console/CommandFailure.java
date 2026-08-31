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
 * Structured command failure with stable classification and location context.
 *
 * @author RollW
 */
public final class CommandFailure {
    private final CommandFailureKind kind;
    private final String message;
    private final CommandPath commandPath;
    private final @Nullable String argumentName;
    private final @Nullable Throwable cause;

    /**
     * Creates a failure without an underlying cause.
     *
     * @param kind         failure category
     * @param message      failure message
     * @param commandPath  command path where the failure occurred
     * @param argumentName optional related option or argument name
     */
    public CommandFailure(CommandFailureKind kind,
                          String message,
                          CommandPath commandPath,
                          @Nullable String argumentName) {
        this(kind, message, commandPath, argumentName, null);
    }

    /**
     * Creates a failure with an optional underlying cause.
     *
     * @param kind         failure category
     * @param message      failure message
     * @param commandPath  command path where the failure occurred
     * @param argumentName optional related option or argument name
     * @param cause        optional underlying cause
     */
    public CommandFailure(CommandFailureKind kind,
                          String message,
                          CommandPath commandPath,
                          @Nullable String argumentName,
                          @Nullable Throwable cause) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.message = requireMessage(message);
        this.commandPath = Objects.requireNonNull(commandPath, "commandPath");
        this.argumentName = normalizeArgumentName(argumentName);
        this.cause = cause;
    }

    /**
     * Creates a usage failure.
     *
     * @param message      failure message
     * @param commandPath  command path where parsing failed
     * @param argumentName optional related option or argument name
     * @return a usage failure
     */
    public static CommandFailure usage(String message,
                                       CommandPath commandPath,
                                       @Nullable String argumentName) {
        return new CommandFailure(CommandFailureKind.USAGE, message, commandPath, argumentName);
    }

    /**
     * Creates an execution failure.
     *
     * @param message     failure message
     * @param commandPath command path whose handler failed
     * @param cause       optional underlying cause
     * @return an execution failure
     */
    public static CommandFailure execution(String message,
                                           CommandPath commandPath,
                                           @Nullable Throwable cause) {
        return new CommandFailure(CommandFailureKind.EXECUTION, message, commandPath, null, cause);
    }

    /**
     * Creates an interaction failure.
     *
     * @param message      failure message
     * @param commandPath  command path whose interaction failed
     * @param argumentName optional related option or argument name
     * @return an interaction failure
     */
    public static CommandFailure interaction(String message,
                                             CommandPath commandPath,
                                             @Nullable String argumentName) {
        return new CommandFailure(CommandFailureKind.INTERACTION, message, commandPath, argumentName);
    }

    /**
     * Creates an unexpected framework failure.
     *
     * @param message     failure message
     * @param commandPath command path where the framework failed
     * @param cause       optional underlying cause
     * @return an internal failure
     */
    public static CommandFailure internal(String message,
                                          CommandPath commandPath,
                                          @Nullable Throwable cause) {
        return new CommandFailure(CommandFailureKind.INTERNAL, message, commandPath, null, cause);
    }

    /**
     * @return the stable failure kind
     */
    public CommandFailureKind getKind() {
        return kind;
    }

    /**
     * @return the user-facing failure message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return the command path at which the failure occurred
     */
    public CommandPath getCommandPath() {
        return commandPath;
    }

    /**
     * @return the optional argument identity related to the failure
     */
    public @Nullable String getArgumentName() {
        return argumentName;
    }

    /**
     * @return the default exit code for this failure
     */
    public int getExitCode() {
        return kind.getDefaultExitCode();
    }

    /**
     * @return the optional underlying cause, never rendered automatically
     */
    public @Nullable Throwable getCause() {
        return cause;
    }

    private static String requireMessage(String message) {
        Objects.requireNonNull(message, "message");
        String normalized = message.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Failure message cannot be blank");
        }
        return normalized;
    }

    private static @Nullable String normalizeArgumentName(@Nullable String argumentName) {
        if (argumentName == null) {
            return null;
        }
        String normalized = argumentName.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Argument name cannot be blank");
        }
        return normalized;
    }
}
