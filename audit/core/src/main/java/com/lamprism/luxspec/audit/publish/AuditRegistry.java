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

import com.lamprism.luxspec.audit.AuditNameNormalizer;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Looks up audit translators by validated event name.
 *
 * <p>The interface is the runtime role. The built-in {@link #builder()}
 * creates an immutable registry, while applications may provide an
 * implementation backed by another immutable or lifecycle-managed source.</p>
 *
 * @author RollW
 */
public interface AuditRegistry {
    /**
     * Finds a translator for an event name.
     *
     * @param eventName the event name
     * @return the translator, or {@code null} when this registry has no match
     */
    @Nullable
    AuditEventTranslator<?> find(String eventName);

    /**
     * Creates the built-in immutable registry builder.
     *
     * @return a mutable assembly builder
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Assembles one immutable registry.
     *
     * <p>The builder is an application assembly tool and is not safe for
     * concurrent runtime mutation. A built registry has no add, remove, or
     * replacement operations.</p>
     */
    final class Builder {
        private static final AuditNameNormalizer EVENT_NAME_NORMALIZER = AuditNameNormalizer.instance();
        private final Map<String, AuditEventTranslator<?>> translators = new LinkedHashMap<>();

        private Builder() {
        }

        /**
         * Registers one translator during application assembly.
         *
         * @param eventName  the event name
         * @param translator the event translator
         * @param <E>        the event payload type
         * @return this builder
         */
        public <E> Builder register(String eventName, AuditEventTranslator<E> translator) {
            String normalized = EVENT_NAME_NORMALIZER.normalize(eventName);
            AuditEventTranslator<E> nonNullTranslator = Objects.requireNonNull(translator, "translator");
            if (translators.putIfAbsent(normalized, nonNullTranslator) != null) {
                throw new IllegalArgumentException("Audit translator is registered more than once: " + normalized);
            }
            return this;
        }

        /**
         * Builds the immutable registry.
         *
         * @return the immutable registry
         */
        public AuditRegistry build() {
            return new ImmutableAuditRegistry(translators);
        }
    }
}
