package com.lamprism.luxspec.event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Synchronously dispatches events by their exact runtime type.
 *
 * @author RollW
 */
public final class EventDispatcher implements EventPublisher {
    private final Map<Class<?>, CopyOnWriteArrayList<ListenerRegistration<?>>> registrations = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();
    private final EventDispatchErrorHandler errorHandler;

    /**
     * Creates a synchronous dispatcher with a listener-failure callback.
     *
     * @param errorHandler the failure callback
     */
    public EventDispatcher(EventDispatchErrorHandler errorHandler) {
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
    }

    /**
     * Registers an exact-runtime-type listener with deterministic ordering.
     *
     * @param eventType the event type
     * @param order the explicit listener order
     * @param listener the listener to register
     * @param <E> the event type
     * @return a subscription that unregisters the listener when closed
     */
    public <E> EventSubscription subscribe(
            Class<E> eventType,
            int order,
            EventListener<? super E> listener
    ) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(listener, "listener");
        ListenerRegistration<E> registration = new ListenerRegistration<>(eventType, order, sequence.getAndIncrement(), listener);
        CopyOnWriteArrayList<ListenerRegistration<?>> listeners = registrations.computeIfAbsent(
                eventType,
                ignored -> new CopyOnWriteArrayList<>()
        );
        listeners.add(registration);
        listeners.sort(
                Comparator.comparingInt((ListenerRegistration<?> candidate) -> candidate.getOrder())
                        .thenComparingLong(ListenerRegistration::getSequence)
        );
        return new Subscription(registration, listeners);
    }

    @Override
    public void publish(Object event) {
        Objects.requireNonNull(event, "event");
        List<ListenerRegistration<?>> snapshot = new ArrayList<>(registrations.getOrDefault(event.getClass(), new CopyOnWriteArrayList<>()));
        for (ListenerRegistration<?> registration : snapshot) {
            dispatch(event, registration);
        }
    }

    @SuppressWarnings("unchecked")
    private <E> void dispatch(Object event, ListenerRegistration<E> registration) {
        try {
            registration.getListener().onEvent((E) event);
        } catch (Throwable failure) {
            errorHandler.onFailure(event, registration.getListener(), failure);
        }
    }

    private static final class ListenerRegistration<E> {
        private final Class<E> eventType;
        private final int order;
        private final long sequence;
        private final EventListener<? super E> listener;

        private ListenerRegistration(Class<E> eventType, int order, long sequence, EventListener<? super E> listener) {
            this.eventType = eventType;
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

    private static final class Subscription implements EventSubscription {
        private final ListenerRegistration<?> registration;
        private final CopyOnWriteArrayList<ListenerRegistration<?>> listeners;
        private boolean closed;

        private Subscription(ListenerRegistration<?> registration, CopyOnWriteArrayList<ListenerRegistration<?>> listeners) {
            this.registration = registration;
            this.listeners = listeners;
        }

        @Override
        public synchronized void close() {
            if (closed) {
                return;
            }
            listeners.remove(registration);
            closed = true;
        }
    }
}
