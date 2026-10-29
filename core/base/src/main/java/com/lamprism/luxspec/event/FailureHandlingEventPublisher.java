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

import com.lamprism.luxspec.failure.FailureHandler;

import java.util.Objects;

/**
 * Applies an explicit failure handler to a delegate event publisher.
 *
 * <p>Runtime publication failures are passed to the handler. Returning from the handler permits
 * the caller to continue; a handler can instead propagate a failure. JVM errors are not handled.
 * This decorator does not make event publication transactional.</p>
 *
 * @author RollW
 */
public final class FailureHandlingEventPublisher implements EventPublisher {
    private final EventPublisher delegate;
    private final FailureHandler<Event> failureHandler;

    /**
     * Creates a publisher with an explicit failure reporting policy.
     *
     * @param delegate       the event publisher
     * @param failureHandler the handler for ordinary publication failures
     */
    public FailureHandlingEventPublisher(EventPublisher delegate, FailureHandler<Event> failureHandler) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.failureHandler = Objects.requireNonNull(failureHandler, "failureHandler");
    }

    @Override
    public void publish(Event event) {
        Event value = Objects.requireNonNull(event, "event");
        try {
            delegate.publish(value);
        } catch (RuntimeException failure) {
            try {
                failureHandler.onFailure(value, failure);
            } catch (RuntimeException | Error handlerFailure) {
                if (handlerFailure != failure) {
                    handlerFailure.addSuppressed(failure);
                }
                throw handlerFailure;
            }
        }
    }
}
