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
 * An action selected by a command parser.
 *
 * @author RollW
 */
public sealed interface CommandAction permits CommandAction.Invoke, CommandAction.Help, CommandAction.Version {
    /**
     * Selects a parsed command invocation.
     *
     * @param invocation the invocation belonging to the application catalog
     * @return the invocation action
     */
    static CommandAction invoke(CommandInvocation invocation) {
        return new Invoke(invocation);
    }

    /**
     * Selects help for a canonical command path.
     *
     * @param path the canonical command path
     * @return the help action
     */
    static CommandAction help(CommandPath path) {
        return new Help(path);
    }

    /**
     * Selects application version information.
     *
     * @return the version action
     */
    static CommandAction version() {
        return Version.INSTANCE;
    }

    /**
     * Executes a parsed invocation.
     */
    final class Invoke implements CommandAction {
        private final CommandInvocation invocation;

        private Invoke(CommandInvocation invocation) {
            this.invocation = Objects.requireNonNull(invocation, "invocation");
        }

        /**
         * @return the parsed invocation
         */
        public CommandInvocation getInvocation() {
            return invocation;
        }
    }

    /**
     * Displays help for a canonical path.
     */
    final class Help implements CommandAction {
        private final CommandPath path;

        private Help(CommandPath path) {
            this.path = Objects.requireNonNull(path, "path");
        }

        /**
         * @return the canonical command path
         */
        public CommandPath getPath() {
            return path;
        }
    }

    /**
     * Displays the application version.
     */
    final class Version implements CommandAction {
        private static final Version INSTANCE = new Version();

        private Version() {
        }
    }
}
