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
import com.lamprism.luxspec.event.Event;
import com.lamprism.luxspec.event.EventDispatcher;
import com.lamprism.luxspec.event.EventSubscription;
import com.lamprism.luxspec.event.EventType;

import java.util.Objects;

/**
 * Unified event type, audit name, and translator definition.
 *
 * <p>The definition is the shared assembly contract for registry entries and
 * event subscriptions. A feature-owned contributor decides when it is
 * included in an application assembly.</p>
 *
 * @param <E> the event payload type
 * @author RollW
 */
public final class AuditEventDefinition<E extends Event> {
    private static final AuditNameNormalizer EVENT_NAME_NORMALIZER = AuditNameNormalizer.instance();
    private final String eventName;
    private final EventType<E> eventType;
    private final AuditEventTranslator<E> translator;

    private AuditEventDefinition(
            String eventName,
            EventType<E> eventType,
            AuditEventTranslator<E> translator
    ) {
        this.eventName = EVENT_NAME_NORMALIZER.normalize(eventName);
        this.eventType = Objects.requireNonNull(eventType, "eventType");
        this.translator = Objects.requireNonNull(translator, "translator");
    }

    /**
     * Creates an event definition.
     *
     * @param eventName  the stable audit event name
     * @param eventType  the exact runtime event class
     * @param translator the configured translator instance for the event type; it is reused for
     *                   every publication and therefore must be safe for the publisher's calling
     *                   model
     * @param <E>        the event payload type
     * @return the immutable definition
     */
    public static <E extends Event> AuditEventDefinition<E> of(
            String eventName,
            Class<E> eventType,
            AuditEventTranslator<E> translator
    ) {
        return of(eventName, EventType.of(eventType), translator);
    }

    /**
     * Creates an event definition for an explicit runtime event channel.
     *
     * @param eventName  the stable audit event name
     * @param eventType  the runtime event channel, including captured generic arguments when used
     * @param translator the configured translator instance for the event type
     * @param <E>        the event payload type
     * @return the immutable definition
     */
    public static <E extends Event> AuditEventDefinition<E> of(
            String eventName,
            EventType<E> eventType,
            AuditEventTranslator<E> translator
    ) {
        return new AuditEventDefinition<>(eventName, eventType, translator);
    }

    /**
     * Returns the stable audit event name.
     *
     * @return the audit event name
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * Returns the runtime event channel handled by this definition.
     *
     * @return the event type token
     */
    public EventType<E> getEventType() {
        return eventType;
    }

    /**
     * Returns the configured translator associated with this definition.
     *
     * @return the event translator
     */
    public AuditEventTranslator<E> getTranslator() {
        return translator;
    }

    /**
     * Adds this definition to a registry builder.
     *
     * @param registry the target registry builder
     * @return the same builder
     */
    public AuditRegistry.Builder register(AuditRegistry.Builder registry) {
        return Objects.requireNonNull(registry, "registry").register(eventName, translator);
    }

    /**
     * Subscribes this definition to an exact-type event dispatcher.
     *
     * @param dispatcher the dispatcher that owns the subscription
     * @param publisher  the audit publisher
     * @return the closeable subscription
     */
    public EventSubscription subscribe(EventDispatcher dispatcher, AuditPublisher publisher) {
        EventDispatcher nonNullDispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        AuditPublisher nonNullPublisher = Objects.requireNonNull(publisher, "publisher");
        return nonNullDispatcher.subscribe(
                eventType,
                0,
                event -> nonNullPublisher.publish(eventName, event)
        );
    }
}
