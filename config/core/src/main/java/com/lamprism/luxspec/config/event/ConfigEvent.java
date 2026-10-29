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

package com.lamprism.luxspec.config.event;

import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.event.Event;

/**
 * Common value-free contract for configuration change events.
 *
 * @author RollW
 */
public interface ConfigEvent extends Event {
    /**
     * Identifies the level at which a configuration event was produced.
     */
    enum Kind {
        /**
         * A raw Source mutation completed.
         */
        SOURCE_CHANGED,
        /**
         * The effective typed result changed.
         */
        EFFECTIVE_CHANGED
    }

    /**
     * Returns the affected complete key.
     *
     * @return the configuration key
     */
    ConfigKey getKey();

    /**
     * Returns the event kind.
     *
     * @return the event kind
     */
    Kind getKind();
}
