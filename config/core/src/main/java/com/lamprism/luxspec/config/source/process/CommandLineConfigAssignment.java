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

package com.lamprism.luxspec.config.source.process;

import java.util.Objects;

/**
 * One configuration assignment parsed from a command-line argument position.
 *
 * @param argumentIndex    the zero-based source argument position
 * @param name             the complete external configuration name
 * @param value            the raw assignment value
 * @param hasExplicitValue whether the argument explicitly supplied a value
 *
 * @author RollW
 */
public record CommandLineConfigAssignment(
        int argumentIndex,
        String name,
        String value,
        boolean hasExplicitValue
) {
    /**
     * Creates one validated command-line assignment.
     *
     * @param argumentIndex    the zero-based source argument position
     * @param name             the complete external configuration name
     * @param value            the raw assignment value
     * @param hasExplicitValue whether the argument explicitly supplied a value
     */
    public CommandLineConfigAssignment {
        if (argumentIndex < 0) {
            throw new IllegalArgumentException("argumentIndex must not be negative");
        }
        name = ProcessConfigName.requireName(name, "name");
        value = Objects.requireNonNull(value, "value");
    }
}
