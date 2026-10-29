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
import com.lamprism.luxspec.config.source.ConfigSourceId;

import java.util.Objects;

/**
 * Reports a completed source mutation without exposing a raw configuration value.
 *
 * @author RollW
 */
public final class ConfigSourceChangedEvent implements ConfigEvent {
    private final ConfigSourceId sourceId;
    private final ConfigKey key;
    private final ConfigSourceChangeType changeType;

    /**
     * Creates a source mutation event.
     *
     * @param sourceId   the source that completed the mutation
     * @param key        the affected configuration key
     * @param changeType the completed mutation type
     */
    public ConfigSourceChangedEvent(ConfigSourceId sourceId, ConfigKey key, ConfigSourceChangeType changeType) {
        this.sourceId = Objects.requireNonNull(sourceId, "sourceId");
        this.key = Objects.requireNonNull(key, "key");
        this.changeType = Objects.requireNonNull(changeType, "changeType");
    }

    /**
     * Returns the source that completed the mutation.
     *
     * @return the source identifier
     */
    public ConfigSourceId getSourceId() {
        return sourceId;
    }

    /**
     * Returns the affected complete configuration key.
     *
     * @return the configuration key
     */
    public ConfigKey getKey() {
        return key;
    }

    @Override
    public Kind getKind() {
        return Kind.SOURCE_CHANGED;
    }

    /**
     * Returns the completed raw mutation type.
     *
     * @return the mutation type
     */
    public ConfigSourceChangeType getChangeType() {
        return changeType;
    }
}
