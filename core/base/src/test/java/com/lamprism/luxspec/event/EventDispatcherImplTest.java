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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventDispatcherImplTest {
    @Test
    void dispatchesTypedEventsInOrderAndReportsListenerFailures() {
        List<String> calls = new ArrayList<>();
        List<Throwable> failures = new ArrayList<>();
        EventDispatcher dispatcher = new EventDispatcherImpl(
                (event, listener, failure) -> failures.add(failure)
        );

        dispatcher.subscribe(TestEvent.class, 10, event -> calls.add("late:" + event.value()));
        dispatcher.subscribe(TestEvent.class, 0, event -> calls.add("early:" + event.value()));
        dispatcher.subscribe(TestEvent.class, 20, event -> {
            throw new IllegalStateException("listener failure");
        });

        dispatcher.publish(new TestEvent("value"));

        assertEquals(List.of("early:value", "late:value"), calls);
        assertEquals(1, failures.size());
        assertEquals(IllegalStateException.class, failures.get(0).getClass());
    }

    @Test
    void removesListenersWhenSubscriptionsClose() {
        List<String> calls = new ArrayList<>();
        EventDispatcher dispatcher = new EventDispatcherImpl((event, listener, failure) -> {
            throw new AssertionError("Listener failed", failure);
        });

        EventSubscription subscription = dispatcher.subscribe(
                TestEvent.class,
                0,
                event -> calls.add(event.value())
        );
        assertTrue(subscription.isActive());
        dispatcher.publish(new TestEvent("first"));
        subscription.unsubscribe();
        assertFalse(subscription.isActive());
        subscription.close();
        dispatcher.publish(new TestEvent("second"));

        assertEquals(List.of("first"), calls);
    }

    @Test
    void combinesSubscriptionsIntoOneLifecycleHandle() {
        EventDispatcher dispatcher = new EventDispatcherImpl((event, listener, failure) -> {
            throw new AssertionError("Listener failed", failure);
        });
        EventSubscription first = dispatcher.subscribe(TestEvent.class, 0, event -> {
        });
        EventSubscription second = dispatcher.subscribe(TestEvent.class, 0, event -> {
        });
        EventSubscription combined = EventSubscription.combine(List.of(first, second));

        assertTrue(combined.isActive());
        combined.unsubscribe();

        assertFalse(combined.isActive());
        assertFalse(first.isActive());
        assertFalse(second.isActive());
        combined.unsubscribe();
    }

    private record TestEvent(String value) implements Event {
    }
}
