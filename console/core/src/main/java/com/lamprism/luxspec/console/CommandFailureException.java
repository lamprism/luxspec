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
 * Checked failure a command handler may use to return structured diagnostics.
 *
 * @author RollW
 */
public final class CommandFailureException extends Exception {
    /**
     * Structured failure carried by this exception.
     */
    private final CommandFailure failure;

    /**
     * Creates an exception for a structured command failure.
     *
     * @param failure structured command failure
     */
    public CommandFailureException(CommandFailure failure) {
        super(Objects.requireNonNull(failure, "failure").getMessage(), failure.getCause());
        this.failure = failure;
    }

    /**
     * @return the structured command failure
     */
    public CommandFailure getFailure() {
        return failure;
    }
}
