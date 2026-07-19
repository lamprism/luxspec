package com.lamprism.luxspec.event;


/**
 * Handles listener failures after the originating operation has completed.
 *
 * @author RollW
 */
@FunctionalInterface
public interface EventDispatchErrorHandler {
    /**
     * Handles a listener failure after the originating event operation has completed.
     *
     * @param event the event being dispatched
     * @param listener the listener that failed
     * @param failure the listener failure
     */
    void onFailure(Object event, EventListener<?> listener, Throwable failure);
}
