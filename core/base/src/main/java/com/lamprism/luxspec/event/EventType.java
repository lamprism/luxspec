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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Identifies one runtime event channel, including captured generic arguments.
 *
 * <p>A non-generic event type uses {@link #of(Class)}. A parameterized channel is captured through
 * a direct anonymous subclass:</p>
 *
 * <pre>{@code
 * EventType<PayloadEvent<String>> stringEvents =
 *         new EventType<PayloadEvent<String>>() {
 *         };
 * }</pre>
 *
 * <p>Java cannot recover the captured argument from an ordinary event instance. Parameterized
 * publication must therefore supply the same token used for subscription. The token identifies a
 * channel; it does not recursively inspect the event payload to prove that nested values match the
 * captured generic arguments.</p>
 *
 * @param <E> the event type carried by the channel
 * @author RollW
 */
public abstract class EventType<E extends Event> {
    private final Type type;
    private final Class<? extends Event> rawType;

    /**
     * Captures the type argument declared by a direct anonymous subclass.
     */
    protected EventType() {
        this.type = capturedType(getClass());
        this.rawType = rawType(type);
    }

    private EventType(Type type) {
        this.type = Objects.requireNonNull(type, "type");
        this.rawType = rawType(type);
    }

    /**
     * Creates a channel for one non-parameterized event class.
     *
     * @param eventType the event class
     * @param <E>       the event type
     * @return the event type token
     */
    public static <E extends Event> EventType<E> of(Class<E> eventType) {
        return new ClassEventType<>(Objects.requireNonNull(eventType, "eventType"));
    }

    /**
     * Returns the complete reflected channel type.
     *
     * @return the reflected type
     */
    public final Type getType() {
        return type;
    }

    /**
     * Returns the raw event class used to validate publications.
     *
     * @return the raw event class
     */
    public final Class<? extends Event> getRawType() {
        return rawType;
    }

    /**
     * Returns whether an event belongs to the raw class represented by this channel.
     *
     * @param event the event to test
     * @return whether the event has the required raw type
     */
    public final boolean accepts(Event event) {
        return rawType.isInstance(Objects.requireNonNull(event, "event"));
    }

    @Override
    public final boolean equals(Object other) {
        if (!(other instanceof EventType<?> that)) {
            return false;
        }
        return type.equals(that.type);
    }

    @Override
    public final int hashCode() {
        return type.hashCode();
    }

    @Override
    public final String toString() {
        return type.getTypeName();
    }

    private static Type capturedType(Class<?> implementationType) {
        Type genericSuperclass = implementationType.getGenericSuperclass();
        if (!(genericSuperclass instanceof ParameterizedType parameterizedType)
                || parameterizedType.getRawType() != EventType.class) {
            throw new IllegalStateException("EventType must be created as a direct anonymous subclass");
        }
        return parameterizedType.getActualTypeArguments()[0];
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Event> rawType(Type type) {
        Type candidate = type;
        if (candidate instanceof ParameterizedType parameterizedType) {
            candidate = parameterizedType.getRawType();
        }
        if (!(candidate instanceof Class<?> rawClass) || !Event.class.isAssignableFrom(rawClass)) {
            throw new IllegalArgumentException("Event type must resolve to an Event class");
        }
        return (Class<? extends Event>) rawClass;
    }

    private static final class ClassEventType<E extends Event> extends EventType<E> {
        private ClassEventType(Class<E> eventType) {
            super(eventType);
        }
    }
}
