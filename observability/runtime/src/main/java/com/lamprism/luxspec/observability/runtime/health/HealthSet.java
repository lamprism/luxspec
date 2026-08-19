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

package com.lamprism.luxspec.observability.runtime.health;

import com.lamprism.luxspec.observability.runtime.ObservabilitySet;

import java.util.Objects;

/**
 * A health-only observability domain contribution.
 *
 * @author RollW
 */
@FunctionalInterface
public interface HealthSet extends ObservabilitySet {
    /**
     * Registers health contributors and group membership.
     *
     * @param builder the health builder
     */
    void register(HealthRegistryBuilder builder);

    @Override
    default void registerHealth(HealthRegistryBuilder builder) {
        register(Objects.requireNonNull(builder, "builder"));
    }
}
