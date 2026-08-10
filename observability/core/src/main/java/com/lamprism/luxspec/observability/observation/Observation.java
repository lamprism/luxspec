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

package com.lamprism.luxspec.observability.observation;

/**
 * Thread-independent operation observation lifecycle handle.
 *
 * @author RollW
 */
public interface Observation extends AutoCloseable {
    ObservationSpec spec();

    ObservationOutcome outcome();

    <T> void put(ObservationAttributeSpec<T> attribute, T value);

    void event(ObservationEventName eventName, ObservationAttributeSet attributes);

    void event(ObservationEventName eventName);

    void error(Throwable error);

    void setOutcome(ObservationOutcome outcome);

    ObservationScope openScope();

    void stop();

    @Override
    void close();
}
