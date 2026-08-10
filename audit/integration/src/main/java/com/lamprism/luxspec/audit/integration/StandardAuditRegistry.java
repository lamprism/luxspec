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

package com.lamprism.luxspec.audit.integration;

import com.lamprism.luxspec.audit.publish.AuditRegistry;

import java.util.Objects;

/**
 * Creates the immutable registry of standard Luxspec audit translators.
 *
 * <p>The returned registry contains only built-in integration translators.
 * Use {@link #register(AuditRegistry.Builder)} when application translators
 * must share the same registry.</p>
 *
 * @author RollW
 */
public final class StandardAuditRegistry {
    private StandardAuditRegistry() {
    }

    /**
     * Creates a registry containing every standard Luxspec audit translator.
     *
     * @return the immutable standard translator registry
     */
    public static AuditRegistry create() {
        return register(AuditRegistry.builder()).build();
    }

    /**
     * Registers every standard translator into an application-owned builder.
     *
     * @param registry the builder that receives standard translators
     * @return the same builder for fluent application registration
     */
    public static AuditRegistry.Builder register(AuditRegistry.Builder registry) {
        AuditRegistry.Builder nonNullRegistry = Objects.requireNonNull(registry, "registry");
        for (StandardAuditEventDefinition<?> definition : StandardAuditEventDefinitions.all()) {
            definition.register(nonNullRegistry);
        }
        return nonNullRegistry;
    }
}
