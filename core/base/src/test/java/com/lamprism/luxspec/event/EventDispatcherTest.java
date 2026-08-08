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

class EventDispatcherTest {
    @Test
    void dispatchesTypedEventsInOrderAndReportsListenerFailures() {
        List<String> calls = new ArrayList<>();
        List<Throwable> failures = new ArrayList<>();
        EventDispatcher dispatcher = new EventDispatcher(
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

    private record TestEvent(String value) implements Event {
    }
}
