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

package com.lamprism.luxspec.config.resolution;

import com.lamprism.luxspec.config.source.ConfigSourceId;

import java.util.Objects;

/**
 * Describes where a configuration result originated.
 *
 * @author RollW
 */
public sealed interface ConfigValueOrigin
        permits ConfigValueOrigin.SourceOrigin,
        ConfigValueOrigin.DefaultOrigin,
        ConfigValueOrigin.AbsentOrigin,
        ConfigValueOrigin.TombstoneOrigin {
    /**
     * Identifies a result observed from one source instance.
     *
     * @param sourceId the non-null source ID
     */
    record SourceOrigin(ConfigSourceId sourceId) implements ConfigValueOrigin {
        /**
         * Creates a source origin.
         *
         * @param sourceId the source ID
         */
        public SourceOrigin {
            Objects.requireNonNull(sourceId, "sourceId");
        }
    }

    /**
     * Identifies a value supplied by the Spec default.
     */
    enum DefaultOrigin implements ConfigValueOrigin {
        /**
         * The Spec default supplied the value.
         */
        INSTANCE
    }

    /**
     * Identifies an absence after all eligible observations were evaluated.
     */
    enum AbsentOrigin implements ConfigValueOrigin {
        /**
         * No source value or default was available.
         */
        INSTANCE
    }

    /**
     * Identifies a source tombstone that suppresses lower-priority fallback.
     *
     * @param sourceId the non-null tombstone source ID
     */
    record TombstoneOrigin(ConfigSourceId sourceId) implements ConfigValueOrigin {
        /**
         * Creates a tombstone origin.
         *
         * @param sourceId the source ID
         */
        public TombstoneOrigin {
            Objects.requireNonNull(sourceId, "sourceId");
        }
    }
}
