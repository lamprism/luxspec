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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Synchronously dispatches events by their exact runtime type.
 *
 * <p>Registration changes hold the write lock. Publication uses the read lock only to capture an
 * immutable listener snapshot, then invokes callbacks after releasing the lock.</p>
 *
 * @author RollW
 */
public class SynchronousEventDispatcher implements EventDispatcher {
    private static final Comparator<ListenerRegistration<?>> REGISTRATION_ORDER =
            Comparator.comparingInt((ListenerRegistration<?> candidate) -> candidate.getOrder())
                    .thenComparingLong(ListenerRegistration::getSequence);

    private final ReadWriteLock registrationLock = new ReentrantReadWriteLock();
    private final Map<Class<?>, List<ListenerRegistration<?>>> registrations = new HashMap<>();
    private final EventDispatchErrorHandler errorHandler;
    private long nextSequence;

    /**
     * Creates a synchronous dispatcher with a listener-failure callback.
     *
     * @param errorHandler the failure callback
     */
    public SynchronousEventDispatcher(EventDispatchErrorHandler errorHandler) {
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
    }

    @Override
    public <E extends Event> EventSubscription subscribe(
            Class<E> eventType,
            int order,
            EventListener<? super E> listener
    ) {
        Class<E> nonNullEventType = Objects.requireNonNull(eventType, "eventType");
        EventListener<? super E> nonNullListener = Objects.requireNonNull(listener, "listener");
        registrationLock.writeLock().lock();
        try {
            ListenerRegistration<E> registration = new ListenerRegistration<>(
                    order,
                    nextSequence++,
                    nonNullListener
            );
            List<ListenerRegistration<?>> listeners = new ArrayList<>(
                    registrations.getOrDefault(nonNullEventType, List.of())
            );
            listeners.add(registration);
            listeners.sort(REGISTRATION_ORDER);
            registrations.put(nonNullEventType, List.copyOf(listeners));
            return new Subscription(nonNullEventType, registration);
        } finally {
            registrationLock.writeLock().unlock();
        }
    }

    @Override
    public void publish(Event event) {
        Event nonNullEvent = Objects.requireNonNull(event, "event");
        List<ListenerRegistration<?>> listeners;
        registrationLock.readLock().lock();
        try {
            listeners = registrations.get(nonNullEvent.getClass());
        } finally {
            registrationLock.readLock().unlock();
        }
        if (listeners == null) {
            return;
        }
        for (ListenerRegistration<?> registration : listeners) {
            dispatch(nonNullEvent, registration);
        }
    }

    private void unsubscribe(Class<?> eventType, ListenerRegistration<?> registration) {
        registrationLock.writeLock().lock();
        try {
            List<ListenerRegistration<?>> listeners = registrations.get(eventType);
            if (listeners == null) {
                return;
            }
            List<ListenerRegistration<?>> remaining = new ArrayList<>(listeners);
            if (!remaining.remove(registration)) {
                return;
            }
            if (remaining.isEmpty()) {
                registrations.remove(eventType);
                return;
            }
            registrations.put(eventType, List.copyOf(remaining));
        } finally {
            registrationLock.writeLock().unlock();
        }
    }

    @SuppressWarnings("unchecked")
    private <E extends Event> void dispatch(Event event, ListenerRegistration<E> registration) {
        try {
            registration.getListener().onEvent((E) event);
        } catch (RuntimeException failure) {
            errorHandler.onFailure(event, registration.getListener(), failure);
        }
    }

    private static final class ListenerRegistration<E extends Event> {
        private final int order;
        private final long sequence;
        private final EventListener<? super E> listener;

        private ListenerRegistration(int order, long sequence, EventListener<? super E> listener) {
            this.order = order;
            this.sequence = sequence;
            this.listener = listener;
        }

        private int getOrder() {
            return order;
        }

        private long getSequence() {
            return sequence;
        }

        private EventListener<? super E> getListener() {
            return listener;
        }
    }

    private final class Subscription implements EventSubscription {
        private final Class<?> eventType;
        private final ListenerRegistration<?> registration;
        private final AtomicBoolean active = new AtomicBoolean(true);

        private Subscription(Class<?> eventType, ListenerRegistration<?> registration) {
            this.eventType = eventType;
            this.registration = registration;
        }

        @Override
        public boolean isActive() {
            return active.get();
        }

        @Override
        public void unsubscribe() {
            if (!active.compareAndSet(true, false)) {
                return;
            }
            SynchronousEventDispatcher.this.unsubscribe(eventType, registration);
        }
    }
}
