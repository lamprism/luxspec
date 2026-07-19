package com.lamprism.luxspec.event;

/**
 * Receives one immutable event type.
 *
 * @param <E> the event type
 * @author RollW
 */
@FunctionalInterface
public interface EventListener<E> {
    /**
     * Receives one event instance.
     *
     * @param event the event instance
     */
    void onEvent(E event);
}
