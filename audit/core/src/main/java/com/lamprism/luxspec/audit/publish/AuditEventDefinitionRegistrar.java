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

package com.lamprism.luxspec.audit.publish;

import java.util.Objects;

/**
 * Receives audit event definitions during application assembly.
 *
 * @author RollW
 */
@FunctionalInterface
public interface AuditEventDefinitionRegistrar {
    /**
     * Registers one event definition with the current assembly.
     *
     * @param definition the event definition
     */
    void register(AuditEventDefinition<?> definition);

    /**
     * Registers definitions in iteration order.
     *
     * @param definitions the event definitions
     */
    default void registerAll(Iterable<? extends AuditEventDefinition<?>> definitions) {
        for (AuditEventDefinition<?> definition : Objects.requireNonNull(definitions, "definitions")) {
            register(Objects.requireNonNull(definition, "definition"));
        }
    }
}
