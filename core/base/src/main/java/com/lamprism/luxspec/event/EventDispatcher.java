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

package com.lamprism.luxspec.event;

/**
 * Publishes events and owns typed listener subscriptions.
 *
 * <p>Publication operates on one complete listener snapshot. A publish that begins after a
 * successful subscription or unsubscription observes that completed change. A publish overlapping
 * the change may observe either snapshot, but never a partially ordered registration set.</p>
 *
 * @author RollW
 */
public interface EventDispatcher extends EventPublisher {
    /**
     * Registers an exact-runtime-type listener with deterministic ordering.
     *
     * <p>Listeners run by ascending {@code order}, then by registration order when their explicit
     * order is equal. This convenience method creates a raw-class {@link EventType}. Use the token
     * overload when erased generic arguments require distinct runtime channels.</p>
     *
     * @param eventClass the event class
     * @param order     the explicit listener order
     * @param listener  the listener to register
     * @param <E>       the event type
     * @return a subscription that unregisters the listener when closed
     */
    default <E extends Event> EventSubscription subscribe(
            Class<E> eventClass,
            int order,
            EventListener<? super E> listener
    ) {
        return subscribe(EventType.of(eventClass), order, listener);
    }

    /**
     * Registers a listener for one explicit runtime event channel.
     *
     * <p>Parameterized tokens captured by {@link EventType} remain distinct even when their raw
     * Java class is the same.</p>
     *
     * @param eventType the runtime event channel
     * @param order     the explicit listener order
     * @param listener  the listener to register
     * @param <E>       the event type
     * @return a subscription that unregisters the listener when closed
     */
    <E extends Event> EventSubscription subscribe(
            EventType<E> eventType,
            int order,
            EventListener<? super E> listener
    );

    /**
     * Publishes an event through an explicit runtime channel.
     *
     * @param eventType the runtime event channel
     * @param event     the event payload
     * @param <E>       the event type
     */
    <E extends Event> void publish(EventType<E> eventType, E event);
}
