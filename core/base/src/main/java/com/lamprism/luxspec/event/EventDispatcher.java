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
 * @author RollW
 */
public interface EventDispatcher extends EventPublisher {
    /**
     * Registers an exact-runtime-type listener with deterministic ordering.
     *
     * @param eventType the event type
     * @param order     the explicit listener order
     * @param listener  the listener to register
     * @param <E>       the event type
     * @return a subscription that unregisters the listener when closed
     */
    <E extends Event> EventSubscription subscribe(
            Class<E> eventType,
            int order,
            EventListener<? super E> listener
    );
}
