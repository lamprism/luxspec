package com.lamprism.luxspec.event;


/**
 * Publishes immutable events to registered listeners.
 *
 * @author RollW
 */
public interface EventPublisher {
    /**
     * Dispatches one immutable event to registered listeners.
     *
     * @param event the event to publish
     */
    void publish(Object event);
}
