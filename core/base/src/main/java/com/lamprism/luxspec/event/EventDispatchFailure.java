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

import java.util.Objects;

/**
 * Identifies the event and listener associated with a dispatch failure.
 *
 * @author RollW
 */
public final class EventDispatchFailure {
    private final Event event;
    private final EventListener<?> listener;

    /**
     * Creates a dispatch failure context.
     *
     * @param event    the event being dispatched
     * @param listener the listener that failed
     */
    public EventDispatchFailure(Event event, EventListener<?> listener) {
        this.event = Objects.requireNonNull(event, "event");
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    /**
     * Returns the event being dispatched.
     *
     * @return the event
     */
    public Event getEvent() {
        return event;
    }

    /**
     * Returns the listener that failed.
     *
     * @return the listener
     */
    public EventListener<?> getListener() {
        return listener;
    }
}
