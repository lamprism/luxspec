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
 * Receives isolated observation lifecycle callbacks in registration order.
 *
 * @author RollW
 */
public interface ObservationHandler {
    default void onStart(ObservationView view) {
    }

    default void onError(ObservationView view) {
    }

    default void onEvent(ObservationView view, ObservationEvent event) {
    }

    default void onScopeOpened(ObservationView view) {
    }

    default void onScopeClosed(ObservationView view) {
    }

    default void onStop(ObservationView view) {
    }
}
