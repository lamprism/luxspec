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
 * Binds one immutable command specification to executable behavior.
 *
 * @author RollW
 */
public final class CommandRegistration {
    private final CommandSpec specification;
    private final CommandRunner runner;

    /**
     * Creates a command registration.
     *
     * @param specification command specification
     * @param runner        executable command runner
     */
    public CommandRegistration(CommandSpec specification, CommandRunner runner) {
        this.specification = Objects.requireNonNull(specification, "specification");
        this.runner = Objects.requireNonNull(runner, "runner");
    }

    /**
     * @return the immutable command specification
     */
    public CommandSpec getSpecification() {
        return specification;
    }

    /**
     * @return the executable command runner
     */
    public CommandRunner getRunner() {
        return runner;
    }

    /**
     * Creates a command registration.
     *
     * @param specification command specification
     * @param runner        executable command runner
     * @return a command registration
     */
    public static CommandRegistration of(CommandSpec specification, CommandRunner runner) {
        return new CommandRegistration(specification, runner);
    }
}
