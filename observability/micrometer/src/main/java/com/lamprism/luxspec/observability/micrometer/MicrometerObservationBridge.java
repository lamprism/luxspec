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

package com.lamprism.luxspec.observability.micrometer;

import com.lamprism.luxspec.observability.observation.ObservationAttributeClassification;
import com.lamprism.luxspec.observability.observation.ObservationAttributeSet;
import com.lamprism.luxspec.observability.observation.ObservationAttributeSpec;
import com.lamprism.luxspec.observability.observation.ObservationEvent;
import com.lamprism.luxspec.observability.observation.ObservationHandler;
import com.lamprism.luxspec.observability.observation.ObservationId;
import com.lamprism.luxspec.observability.observation.ObservationRegistry;
import com.lamprism.luxspec.observability.observation.ObservationView;
import com.lamprism.luxspec.observability.runtime.observation.ObservationRegistryEventSource;
import io.micrometer.observation.Observation;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Projects Luxspec observation lifecycles into Micrometer Observation.
 *
 * @author RollW
 */
public class MicrometerObservationBridge implements AutoCloseable {
    private final io.micrometer.observation.ObservationRegistry observationRegistry;
    private final ObservationRegistryEventSource events;
    private final Map<ObservationId, ProviderObservation> observations = new ConcurrentHashMap<>();
    private final ObservationHandler handler = new BridgeHandler();
    private final AutoCloseable listener;
    private final AtomicBoolean closed = new AtomicBoolean();

    public MicrometerObservationBridge(
            ObservationRegistry observationRegistry,
            io.micrometer.observation.ObservationRegistry providerRegistry
    ) {
        Objects.requireNonNull(observationRegistry, "observationRegistry");
        this.observationRegistry = Objects.requireNonNull(providerRegistry, "providerRegistry");
        if (!(observationRegistry instanceof ObservationRegistryEventSource registryEvents)) {
            throw new IllegalArgumentException("The observation registry does not expose lifecycle events");
        }
        events = registryEvents;
        listener = events.addHandler(handler);
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        try {
            listener.close();
        } catch (Exception failure) {
            throw new MicrometerProjectionException("Unable to detach the Micrometer observation bridge", failure);
        } finally {
            for (ProviderObservation observation : observations.values()) {
                observation.close();
            }
            observations.clear();
        }
    }

    private final class BridgeHandler implements ObservationHandler {
        @Override
        public void onStart(ObservationView view) {
            if (closed.get()) {
                return;
            }
            Observation providerObservation =
                    Observation.createNotStarted(
                            view.spec().getName().value(),
                            observationRegistry
                    );
            ObservationId parentId = view.parentId();
            ProviderObservation parent = parentId == null ? null : observations.get(parentId);
            if (parent != null) {
                providerObservation.parentObservation(parent.observation);
            }
            MicrometerObservationLinkContext.put(providerObservation.getContext(), view.links());
            applyAttributes(providerObservation, view.attributes());
            providerObservation.start();
            observations.put(view.id(), new ProviderObservation(providerObservation));
        }

        @Override
        public void onError(ObservationView view) {
            ProviderObservation observation = observations.get(view.id());
            Throwable error = view.error();
            if (observation != null && error != null) {
                observation.observation.error(error);
            }
        }

        @Override
        public void onEvent(ObservationView view, ObservationEvent event) {
            ProviderObservation observation = observations.get(view.id());
            if (observation == null) {
                return;
            }
            applyAttributes(observation.observation, event.attributes());
            observation.observation.event(Observation.Event.of(
                    event.name().value(),
                    event.name().value(),
                    event.occurredAt().toEpochMilli()
            ));
        }

        @Override
        public void onScopeOpened(ObservationView view) {
            ProviderObservation observation = observations.get(view.id());
            if (observation != null) {
                observation.openScope();
            }
        }

        @Override
        public void onScopeClosed(ObservationView view) {
            ProviderObservation observation = observations.get(view.id());
            if (observation != null) {
                observation.closeScope();
                if (observation.isStopped() && observation.isScopeFree()) {
                    observations.remove(view.id(), observation);
                }
            }
        }

        @Override
        public void onStop(ObservationView view) {
            ProviderObservation observation = observations.get(view.id());
            if (observation == null) {
                return;
            }
            applyAttributes(observation.observation, view.attributes());
            observation.observation.lowCardinalityKeyValue("outcome", view.outcome().name());
            observation.stop();
            if (observation.isScopeFree()) {
                observations.remove(view.id(), observation);
            }
        }
    }

    private static void applyAttributes(
            Observation observation,
            ObservationAttributeSet attributes
    ) {
        for (Map.Entry<ObservationAttributeSpec<?>, Object> entry : attributes.values().entrySet()) {
            ObservationAttributeSpec<?> spec = entry.getKey();
            String value = format(spec, entry.getValue());
            if (spec.classification() == ObservationAttributeClassification.LOW) {
                observation.lowCardinalityKeyValue(spec.name(), value);
            } else {
                observation.highCardinalityKeyValue(spec.name(), value);
            }
        }
    }

    private static String format(ObservationAttributeSpec<?> spec, Object value) {
        return formatUnchecked(spec, value);
    }

    private static <T> String formatUnchecked(ObservationAttributeSpec<T> spec, Object value) {
        return spec.formatValue(spec.valueType().cast(value));
    }

    private static final class ProviderObservation {
        private final Observation observation;
        private final ConcurrentLinkedDeque<Observation.Scope> scopes = new ConcurrentLinkedDeque<>();
        private final AtomicBoolean stopped = new AtomicBoolean();

        private ProviderObservation(Observation observation) {
            this.observation = observation;
        }

        private void openScope() {
            if (!stopped.get()) {
                scopes.push(observation.openScope());
            }
        }

        private void closeScope() {
            Observation.Scope scope = scopes.pollFirst();
            if (scope != null) {
                scope.close();
            }
        }

        private void stop() {
            if (!stopped.compareAndSet(false, true)) {
                return;
            }
            observation.stop();
        }

        private boolean isScopeFree() {
            return scopes.isEmpty();
        }

        private boolean isStopped() {
            return stopped.get();
        }

        private void close() {
            Observation.Scope scope;
            while ((scope = scopes.pollFirst()) != null) {
                scope.close();
            }
            stop();
        }
    }
}
