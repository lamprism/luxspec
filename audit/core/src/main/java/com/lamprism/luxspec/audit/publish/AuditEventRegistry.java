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

import com.lamprism.luxspec.event.Event;
import com.lamprism.luxspec.event.EventDispatcher;
import com.lamprism.luxspec.event.EventSubscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Owns typed event-to-audit subscriptions.
 *
 * <p>{@link AuditRegistry} resolves audit event names to translators. This
 * registry owns the separate runtime concern of subscribing event definitions
 * to an {@link EventDispatcher}. The registry is useful for application-owned
 * event definitions as well as the definitions supplied by an integration
 * catalog.</p>
 *
 * @author RollW
 */
public final class AuditEventRegistry implements AutoCloseable {
    private final EventDispatcher dispatcher;
    private final AuditPublisher publisher;
    private final List<AuditEventDefinition<?>> definitions = new ArrayList<>();
    private final List<EventSubscription> subscriptions = new ArrayList<>();
    private boolean closed;

    /**
     * Creates an empty event registry.
     *
     * @param dispatcher the dispatcher that owns event listeners
     * @param publisher  the publisher that receives translated events
     */
    public AuditEventRegistry(EventDispatcher dispatcher, AuditPublisher publisher) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
    }

    /**
     * Registers one event definition.
     *
     * @param definition the event definition to subscribe
     * @param <E>        the event type
     * @return this registry
     * @throws IllegalStateException when this registry has been closed
     */
    public synchronized <E extends Event> AuditEventRegistry register(
            AuditEventDefinition<E> definition
    ) {
        requireOpen();
        AuditEventDefinition<E> nonNullDefinition = Objects.requireNonNull(definition, "definition");
        EventSubscription subscription = nonNullDefinition.subscribe(dispatcher, publisher);
        subscriptions.add(subscription);
        definitions.add(nonNullDefinition);
        return this;
    }

    /**
     * Registers every definition in the supplied collection.
     *
     * <p>If one subscription fails, subscriptions created by this call are
     * removed and the original failure is rethrown.</p>
     *
     * @param definitions the event definitions to subscribe
     * @return this registry
     * @throws IllegalStateException when this registry has been closed
     */
    public synchronized AuditEventRegistry registerAll(
            Iterable<? extends AuditEventDefinition<?>> definitions
    ) {
        requireOpen();
        Iterable<? extends AuditEventDefinition<?>> nonNullDefinitions = Objects.requireNonNull(
                definitions,
                "definitions"
        );
        int definitionStart = this.definitions.size();
        int subscriptionStart = subscriptions.size();
        try {
            for (AuditEventDefinition<?> definition : nonNullDefinitions) {
                register(definition);
            }
        } catch (RuntimeException failure) {
            rollback(definitionStart, subscriptionStart, failure);
            throw failure;
        }
        return this;
    }

    /**
     * Returns the definitions currently owned by this registry.
     *
     * @return an immutable snapshot of the registered definitions
     */
    public synchronized List<AuditEventDefinition<?>> definitions() {
        return List.copyOf(definitions);
    }

    /**
     * Removes every subscription owned by this registry.
     */
    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        RuntimeException failure = closeFrom(0, null);
        subscriptions.clear();
        if (failure != null) {
            throw failure;
        }
    }

    private void rollback(int definitionStart, int subscriptionStart, RuntimeException failure) {
        RuntimeException cleanupFailure = closeFrom(subscriptionStart, failure);
        while (definitions.size() > definitionStart) {
            definitions.remove(definitions.size() - 1);
        }
        if (cleanupFailure != null && cleanupFailure != failure) {
            failure.addSuppressed(cleanupFailure);
        }
    }

    private RuntimeException closeFrom(int start, RuntimeException failure) {
        RuntimeException currentFailure = failure;
        for (int index = subscriptions.size() - 1; index >= start; index--) {
            try {
                subscriptions.remove(index).close();
            } catch (RuntimeException closeFailure) {
                if (currentFailure == null) {
                    currentFailure = closeFailure;
                } else {
                    currentFailure.addSuppressed(closeFailure);
                }
            }
        }
        return currentFailure;
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("The audit event registry is closed");
        }
    }
}
