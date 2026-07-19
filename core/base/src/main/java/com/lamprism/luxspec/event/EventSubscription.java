package com.lamprism.luxspec.event;

/**
 * Removes one listener registration when closed.
 *
 * @author RollW
 */
public interface EventSubscription extends AutoCloseable {
    @Override
    void close();
}
