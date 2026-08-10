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

import com.lamprism.luxspec.audit.AuditNameValidator;
import com.lamprism.luxspec.audit.publish.AuditEventTranslator;
import com.lamprism.luxspec.audit.publish.AuditRegistry;
import com.lamprism.luxspec.event.Event;

import java.util.Objects;

/**
 * Binds one standard event type to its audit name and translator.
 *
 * @param <E> the event type
 * @author RollW
 */
final class StandardAuditEventDefinition<E extends Event> {
    private final String eventName;
    private final Class<E> eventType;
    private final AuditEventTranslator<E> translator;

    private StandardAuditEventDefinition(
            String eventName,
            Class<E> eventType,
            AuditEventTranslator<E> translator
    ) {
        this.eventName = AuditNameValidator.require(eventName);
        this.eventType = Objects.requireNonNull(eventType, "eventType");
        this.translator = Objects.requireNonNull(translator, "translator");
    }

    static <E extends Event> StandardAuditEventDefinition<E> of(
            String eventName,
            Class<E> eventType,
            AuditEventTranslator<E> translator
    ) {
        return new StandardAuditEventDefinition<>(eventName, eventType, translator);
    }

    String eventName() {
        return eventName;
    }

    Class<E> eventType() {
        return eventType;
    }

    void register(AuditRegistry.Builder registry) {
        registry.register(eventName, translator);
    }
}
