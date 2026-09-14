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

/**
 * Marks an immutable provider-independent notification payload.
 * <p>
 * The contract intentionally contains no lifecycle metadata. Event types define
 * the domain information that their listeners need, while publishers and
 * dispatchers use this marker as the common event boundary. Event-specific
 * identity, time, source, or routing data belongs to the event type that gives
 * that data a stable meaning.
 *
 * @author RollW
 */
public interface Event {
}
