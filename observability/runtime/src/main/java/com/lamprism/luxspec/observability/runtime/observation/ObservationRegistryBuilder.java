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

package com.lamprism.luxspec.observability.runtime.observation;

import com.lamprism.luxspec.observability.ObservabilityClock;
import com.lamprism.luxspec.observability.SystemObservabilityClock;
import com.lamprism.luxspec.observability.observation.ObservationActivation;
import com.lamprism.luxspec.observability.observation.ObservationFilter;
import com.lamprism.luxspec.observability.observation.ObservationHandler;
import com.lamprism.luxspec.observability.observation.ObservationPredicate;
import com.lamprism.luxspec.observability.observation.ObservationRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds the default in-process observation registry.
 *
 * @author RollW
 */
public final class ObservationRegistryBuilder {
    private ObservabilityClock clock = new SystemObservabilityClock();
    private ObservationActivation activation = spec -> true;
    private final List<ObservationPredicate> predicates = new ArrayList<>();
    private final List<ObservationFilter> filters = new ArrayList<>();
    private final List<ObservationHandler> handlers = new ArrayList<>();

    private ObservationRegistryBuilder() {
    }

    public static ObservationRegistryBuilder builder() {
        return new ObservationRegistryBuilder();
    }

    public ObservationRegistryBuilder clock(ObservabilityClock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
        return this;
    }

    public ObservationRegistryBuilder activation(ObservationActivation activation) {
        this.activation = Objects.requireNonNull(activation, "activation");
        return this;
    }

    public ObservationRegistryBuilder predicate(ObservationPredicate predicate) {
        predicates.add(Objects.requireNonNull(predicate, "predicate"));
        return this;
    }

    public ObservationRegistryBuilder filter(ObservationFilter filter) {
        filters.add(Objects.requireNonNull(filter, "filter"));
        return this;
    }

    public ObservationRegistryBuilder handler(ObservationHandler handler) {
        handlers.add(Objects.requireNonNull(handler, "handler"));
        return this;
    }

    public ObservationRegistry build() {
        return new DefaultObservationRegistry(clock, activation, predicates, filters, handlers);
    }
}
