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
import io.micrometer.observation.Observation;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Projects Luxspec observation lifecycles into Micrometer Observation.
 *
 * @author RollW
 */
public class MicrometerObservationBridge implements AutoCloseable {
    private final ObservationRegistry registry;
    private final io.micrometer.observation.ObservationRegistry providerRegistry;
    private final Map<ObservationId, ProviderObservation> observations = new ConcurrentHashMap<>();
    private final ObservationHandler handler = new BridgeHandler();
    private final AutoCloseable listener;
    private final Object lifecycleLock = new Object();
    private int activeScopeCallbacks;
    private boolean closed;
    private boolean listenerDetached;

    public MicrometerObservationBridge(
            ObservationRegistry observationRegistry,
            io.micrometer.observation.ObservationRegistry providerRegistry
    ) {
        registry = Objects.requireNonNull(observationRegistry, "observationRegistry");
        this.providerRegistry = Objects.requireNonNull(providerRegistry, "providerRegistry");
        listener = registry.addHandler(handler);
    }

    @Override
    public void close() {
        synchronized (lifecycleLock) {
            if (listenerDetached) {
                return;
            }
            closed = true;
        }
        RuntimeException closeFailure = null;
        for (ProviderObservation observation : observations.values()) {
            try {
                observation.close();
            } catch (RuntimeException failure) {
                if (closeFailure == null) {
                    closeFailure = failure;
                } else if (closeFailure != failure) {
                    closeFailure.addSuppressed(failure);
                }
            }
        }
        observations.entrySet().removeIf(entry ->
                entry.getValue().isStopped() && entry.getValue().isScopeFree()
        );
        try {
            detachListenerIfIdle();
        } catch (RuntimeException failure) {
            if (closeFailure == null) {
                closeFailure = failure;
            } else if (closeFailure != failure) {
                closeFailure.addSuppressed(failure);
            }
        }
        if (closeFailure != null) {
            throw new MicrometerProjectionException("Unable to close the Micrometer observation bridge", closeFailure);
        }
    }

    private final class BridgeHandler implements ObservationHandler {
        @Override
        public void onStart(ObservationView view) {
            synchronized (lifecycleLock) {
                if (closed) {
                    return;
                }
            }
            Observation providerObservation =
                    Observation.createNotStarted(
                            view.spec().getName().value(),
                            providerRegistry
                    );
            ObservationId parentId = view.parentId();
            ProviderObservation parent = parentId == null ? null : observations.get(parentId);
            if (parent != null) {
                providerObservation.parentObservation(parent.observation);
            }
            MicrometerObservationLinkContext.put(providerObservation.getContext(), view.links());
            applyAttributes(providerObservation, view.attributes());
            providerObservation.start();
            ProviderObservation projectedObservation = new ProviderObservation(providerObservation);
            boolean stopImmediately;
            synchronized (lifecycleLock) {
                stopImmediately = closed;
                if (!stopImmediately) {
                    observations.put(view.id(), projectedObservation);
                }
            }
            if (stopImmediately) {
                projectedObservation.stop();
            }
        }

        @Override
        public void onError(ObservationView view) {
            if (isClosed()) {
                return;
            }
            ProviderObservation observation = observations.get(view.id());
            Throwable error = view.error();
            if (observation != null && error != null) {
                observation.observation.error(error);
            }
        }

        @Override
        public void onEvent(ObservationView view, ObservationEvent event) {
            if (isClosed()) {
                return;
            }
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
            if (!beginScopeCallback()) {
                return;
            }
            try {
                ProviderObservation observation = observations.get(view.id());
                if (observation != null) {
                    observation.openScope();
                }
            } finally {
                endScopeCallback();
            }
        }

        @Override
        public void onScopeClosed(ObservationView view) {
            if (!beginScopeCallback()) {
                return;
            }
            try {
                ProviderObservation observation = observations.get(view.id());
                if (observation != null) {
                    observation.closeScope();
                    if (observation.isStopped() && observation.isScopeFree()) {
                        observations.remove(view.id(), observation);
                    }
                }
            } finally {
                endScopeCallback();
            }
        }

        @Override
        public void onStop(ObservationView view) {
            ProviderObservation observation = observations.get(view.id());
            if (isClosed()) {
                if (observation != null && observation.isStopped() && observation.isScopeFree()) {
                    observations.remove(view.id(), observation);
                }
                detachListenerIfIdle();
                return;
            }
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

    private boolean isClosed() {
        synchronized (lifecycleLock) {
            return closed;
        }
    }

    private boolean beginScopeCallback() {
        synchronized (lifecycleLock) {
            if (listenerDetached) {
                return false;
            }
            activeScopeCallbacks++;
            return true;
        }
    }

    private void endScopeCallback() {
        synchronized (lifecycleLock) {
            activeScopeCallbacks--;
        }
        detachListenerIfIdle();
    }

    private void detachListenerIfIdle() {
        boolean detach;
        synchronized (lifecycleLock) {
            if (!closed || listenerDetached || activeScopeCallbacks != 0) {
                return;
            }
            observations.entrySet().removeIf(entry ->
                    entry.getValue().isStopped() && entry.getValue().isScopeFree()
            );
            detach = observations.isEmpty();
            if (detach) {
                listenerDetached = true;
            }
        }
        if (!detach) {
            return;
        }
        try {
            listener.close();
        } catch (Exception failure) {
            synchronized (lifecycleLock) {
                listenerDetached = false;
            }
            throw new MicrometerProjectionException(
                    "Unable to detach the Micrometer observation bridge",
                    failure
            );
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
        private final Map<Thread, ConcurrentLinkedDeque<ScopeEntry>> scopes = new ConcurrentHashMap<>();
        private final AtomicBoolean stopped = new AtomicBoolean();

        private ProviderObservation(Observation observation) {
            this.observation = observation;
        }

        private synchronized void openScope() {
            Thread owner = Thread.currentThread();
            Optional<Observation.Scope> scope = stopped.get()
                    ? Optional.empty()
                    : Optional.of(observation.openScope());
            scopes.computeIfAbsent(owner, ignored -> new ConcurrentLinkedDeque<>())
                    .push(new ScopeEntry(scope));
        }

        private synchronized void closeScope() {
            Thread owner = Thread.currentThread();
            ConcurrentLinkedDeque<ScopeEntry> threadScopes = scopes.get(owner);
            ScopeEntry scopeEntry = threadScopes == null ? null : threadScopes.pollFirst();
            if (scopeEntry != null) {
                try {
                    scopeEntry.scope().ifPresent(Observation.Scope::close);
                } finally {
                    removeEmptyStack(owner, threadScopes);
                }
            }
        }

        private synchronized void stop() {
            if (!stopped.compareAndSet(false, true)) {
                return;
            }
            observation.stop();
        }

        private synchronized boolean isScopeFree() {
            return scopes.values().stream().allMatch(ConcurrentLinkedDeque::isEmpty);
        }

        private boolean isStopped() {
            return stopped.get();
        }

        private synchronized void close() {
            Thread owner = Thread.currentThread();
            ConcurrentLinkedDeque<ScopeEntry> threadScopes = scopes.get(owner);
            RuntimeException failure = null;
            if (threadScopes != null) {
                ScopeEntry scopeEntry;
                while ((scopeEntry = threadScopes.pollFirst()) != null) {
                    try {
                        scopeEntry.scope().ifPresent(Observation.Scope::close);
                    } catch (RuntimeException closeFailure) {
                        if (failure == null) {
                            failure = closeFailure;
                        } else if (failure != closeFailure) {
                            failure.addSuppressed(closeFailure);
                        }
                    }
                }
                scopes.remove(owner, threadScopes);
            }
            try {
                stop();
            } catch (RuntimeException stopFailure) {
                if (failure == null) {
                    failure = stopFailure;
                } else if (failure != stopFailure) {
                    failure.addSuppressed(stopFailure);
                }
            }
            if (failure != null) {
                throw failure;
            }
        }

        private void removeEmptyStack(Thread owner, ConcurrentLinkedDeque<ScopeEntry> threadScopes) {
            if (threadScopes != null && threadScopes.isEmpty()) {
                scopes.remove(owner, threadScopes);
            }
        }

        private record ScopeEntry(Optional<Observation.Scope> scope) {
        }
    }
}
