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

import com.lamprism.luxspec.audit.publish.AuditPublisher;
import com.lamprism.luxspec.event.Event;
import com.lamprism.luxspec.event.EventDispatcher;
import com.lamprism.luxspec.event.EventSubscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Registers and owns the standard event-to-audit bindings.
 *
 * <p>The registration uses the same definitions as {@link StandardAuditRegistry}, so event types,
 * event names, and translators are assembled from one source. It is an application lifecycle
 * object, not an event type or a general-purpose subscription registry.</p>
 *
 * @author RollW
 */
public final class StandardAuditEventRegistration implements AutoCloseable {
    private final List<EventSubscription> subscriptions;
    private boolean closed;

    /**
     * Registers every standard configuration, security, and user lifecycle event type.
     *
     * @param dispatcher the exact-type event dispatcher
     * @param publisher  the audit publisher
     */
    public StandardAuditEventRegistration(EventDispatcher dispatcher, AuditPublisher publisher) {
        EventDispatcher nonNullDispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        AuditPublisher nonNullPublisher = Objects.requireNonNull(publisher, "publisher");
        List<EventSubscription> registered = new ArrayList<>();
        try {
            for (StandardAuditEventDefinition<?> definition : StandardAuditEventDefinitions.all()) {
                registered.add(register(nonNullDispatcher, definition, nonNullPublisher));
            }
        } catch (RuntimeException failure) {
            close(registered);
            throw failure;
        }
        this.subscriptions = List.copyOf(registered);
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        close(subscriptions);
        closed = true;
    }

    private static <E extends Event> EventSubscription register(
            EventDispatcher dispatcher,
            StandardAuditEventDefinition<E> definition,
            AuditPublisher publisher
    ) {
        return dispatcher.subscribe(
                definition.eventType(),
                0,
                event -> publisher.publish(definition.eventName(), event)
        );
    }

    private static void close(List<EventSubscription> registrations) {
        for (int index = registrations.size() - 1; index >= 0; index--) {
            registrations.get(index).close();
        }
    }
}
