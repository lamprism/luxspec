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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controls the lifecycle of one or more event listener registrations.
 *
 * @author RollW
 */
public interface EventSubscription extends AutoCloseable {
    /**
     * Returns whether this subscription can still receive events.
     *
     * @return {@code true} while the subscription is active
     */
    boolean isActive();

    /**
     * Removes the listener registration represented by this subscription.
     *
     * <p>The operation is idempotent. Calling it more than once has no
     * additional effect. A publication already holding a listener snapshot may still invoke the
     * listener, while one that starts after this method returns must not.</p>
     */
    void unsubscribe();

    /**
     * Combines subscriptions into one lifecycle handle.
     *
     * <p>Members are unsubscribed in reverse order. Every member is given a
     * chance to unsubscribe; later failures are added to the first failure as
     * suppressed exceptions.</p>
     *
     * @param subscriptions the subscriptions to combine
     * @return the combined lifecycle handle
     */
    static EventSubscription combine(Iterable<? extends EventSubscription> subscriptions) {
        List<EventSubscription> members = new ArrayList<>();
        for (EventSubscription subscription : Objects.requireNonNull(subscriptions, "subscriptions")) {
            members.add(Objects.requireNonNull(subscription, "subscription"));
        }
        List<EventSubscription> immutableMembers = List.copyOf(members);
        return new EventSubscription() {
            private final AtomicBoolean active = new AtomicBoolean(true);

            @Override
            public boolean isActive() {
                return active.get();
            }

            @Override
            public void unsubscribe() {
                if (!active.compareAndSet(true, false)) {
                    return;
                }
                RuntimeException failure = null;
                for (int index = immutableMembers.size() - 1; index >= 0; index--) {
                    try {
                        immutableMembers.get(index).unsubscribe();
                    } catch (RuntimeException unsubscribeFailure) {
                        if (failure == null) {
                            failure = unsubscribeFailure;
                        } else {
                            failure.addSuppressed(unsubscribeFailure);
                        }
                    }
                }
                if (failure != null) {
                    throw failure;
                }
            }
        };
    }

    @Override
    default void close() {
        unsubscribe();
    }
}
