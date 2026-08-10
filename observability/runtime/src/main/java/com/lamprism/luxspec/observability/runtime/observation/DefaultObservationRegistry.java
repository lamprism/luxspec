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

import com.lamprism.luxspec.context.ContextKey;
import com.lamprism.luxspec.context.ExecutionContext;
import com.lamprism.luxspec.context.ExecutionContexts;
import com.lamprism.luxspec.observability.ObservabilityClock;
import com.lamprism.luxspec.observability.observation.Observation;
import com.lamprism.luxspec.observability.observation.ObservationActivation;
import com.lamprism.luxspec.observability.observation.ObservationAttributeSet;
import com.lamprism.luxspec.observability.observation.ObservationAttributeSpec;
import com.lamprism.luxspec.observability.observation.ObservationEvent;
import com.lamprism.luxspec.observability.observation.ObservationEventName;
import com.lamprism.luxspec.observability.observation.ObservationFilter;
import com.lamprism.luxspec.observability.observation.ObservationHandler;
import com.lamprism.luxspec.observability.observation.ObservationId;
import com.lamprism.luxspec.observability.observation.ObservationLink;
import com.lamprism.luxspec.observability.observation.ObservationName;
import com.lamprism.luxspec.observability.observation.ObservationOutcome;
import com.lamprism.luxspec.observability.observation.ObservationPredicate;
import com.lamprism.luxspec.observability.observation.ObservationRegistry;
import com.lamprism.luxspec.observability.observation.ObservationScope;
import com.lamprism.luxspec.observability.observation.ObservationSpec;
import com.lamprism.luxspec.observability.observation.ObservationStart;
import com.lamprism.luxspec.observability.observation.ObservationView;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Coordinates observation lifecycle state and handler dispatch.
 *
 * @author RollW
 */
final class DefaultObservationRegistry implements ObservationRegistry, ObservationRegistryEventSource {
    private static final ContextKey<Observation> CURRENT_OBSERVATION = ContextKey.of(
            "luxspec.observability.observation",
            Observation.class
    );

    private final ObservabilityClock clock;
    private final ObservationActivation activation;
    private final List<ObservationPredicate> predicates;
    private final List<ObservationFilter> filters;
    private final CopyOnWriteArrayList<ObservationHandler> handlers;
    private final Map<ObservationName, ObservationSpec> specs = new LinkedHashMap<>();
    private boolean closed;

    DefaultObservationRegistry(
            ObservabilityClock clock,
            ObservationActivation activation,
            List<ObservationPredicate> predicates,
            List<ObservationFilter> filters,
            List<ObservationHandler> handlers
    ) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.activation = Objects.requireNonNull(activation, "activation");
        this.predicates = List.copyOf(predicates);
        this.filters = List.copyOf(filters);
        this.handlers = new CopyOnWriteArrayList<>(handlers);
    }

    @Override
    public synchronized void register(ObservationSpec spec) {
        requireOpen();
        ObservationSpec nonNullSpec = Objects.requireNonNull(spec, "spec");
        ObservationSpec previous = specs.get(nonNullSpec.getName());
        if (previous != null && !previous.equals(nonNullSpec)) {
            throw new IllegalArgumentException("Conflicting observation definition: " + nonNullSpec.getName());
        }
        specs.putIfAbsent(nonNullSpec.getName(), nonNullSpec);
    }

    @Override
    public synchronized @Nullable ObservationSpec find(ObservationName name) {
        requireOpen();
        return specs.get(Objects.requireNonNull(name, "name"));
    }

    @Override
    public synchronized List<ObservationSpec> getSpecs() {
        requireOpen();
        return List.copyOf(specs.values());
    }

    @Override
    public Observation start(ObservationSpec spec) {
        Objects.requireNonNull(spec, "spec");
        return start(ObservationStart.builder(spec).build());
    }

    @Override
    public synchronized Observation start(ObservationStart start) {
        requireOpen();
        ObservationStart current = Objects.requireNonNull(start, "start");
        ObservationSpec registered = specs.get(current.spec().getName());
        if (registered == null || !registered.equals(current.spec())) {
            throw new IllegalStateException("Observation spec is not registered: " + current.spec().getName());
        }
        if (!activation.isEnabled(registered)) {
            return new NoOpObservation(registered);
        }
        for (ObservationPredicate predicate : predicates) {
            try {
                if (!predicate.test(current)) {
                    return new NoOpObservation(registered);
                }
            } catch (Throwable ignored) {
                return new NoOpObservation(registered);
            }
        }
        for (ObservationFilter filter : filters) {
            try {
                ObservationStart filtered = filter.apply(current);
                if (filtered != null) {
                    current = filtered;
                }
            } catch (Throwable ignored) {
                // A failed filter cannot invalidate the operation start.
            }
        }
        ObservationSpec filteredSpec = current.spec();
        ObservationSpec filteredRegistration = specs.get(filteredSpec.getName());
        if (filteredRegistration == null || !filteredRegistration.equals(filteredSpec)) {
            return new NoOpObservation(registered);
        }
        if (!activation.isEnabled(filteredRegistration)) {
            return new NoOpObservation(filteredRegistration);
        }
        Observation parent = resolveParent(current);
        DefaultObservation observation = new DefaultObservation(
                this,
                current.spec(),
                current.attributes(),
                parent,
                current.links()
        );
        observation.dispatchStart();
        return observation;
    }

    @Override
    public synchronized AutoCloseable addHandler(ObservationHandler handler) {
        requireOpen();
        ObservationHandler nonNullHandler = Objects.requireNonNull(handler, "handler");
        handlers.add(nonNullHandler);
        return () -> handlers.remove(nonNullHandler);
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        specs.clear();
        handlers.clear();
    }

    private Observation resolveParent(ObservationStart start) {
        if (start.isRoot()) {
            return null;
        }
        if (start.parent() != null) {
            return start.parent();
        }
        Optional<ExecutionContext> context = ExecutionContexts.current();
        return context.flatMap(value -> value.get(CURRENT_OBSERVATION)).orElse(null);
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Observation registry is closed");
        }
    }

    private static final class NoOpObservation implements Observation {
        private final ObservationSpec spec;

        private NoOpObservation(ObservationSpec spec) {
            this.spec = spec;
        }

        @Override
        public ObservationSpec spec() {
            return spec;
        }

        @Override
        public ObservationOutcome outcome() {
            return ObservationOutcome.UNSET;
        }

        @Override
        public <T> void put(ObservationAttributeSpec<T> attribute, T value) {
        }

        @Override
        public void event(ObservationEventName eventName, ObservationAttributeSet attributes) {
        }

        @Override
        public void event(ObservationEventName eventName) {
        }

        @Override
        public void error(Throwable error) {
        }

        @Override
        public void setOutcome(ObservationOutcome outcome) {
        }

        @Override
        public ObservationScope openScope() {
            return () -> {
            };
        }

        @Override
        public void stop() {
        }

        @Override
        public void close() {
        }
    }

    private static final class DefaultObservation implements Observation {
        private final DefaultObservationRegistry registry;
        private final ObservationId id = ObservationId.generated();
        private final ObservationSpec spec;
        private final @Nullable ObservationId parentId;
        private final Instant startedAt;
        private final long startedNanos;
        private final List<ObservationLink> links;
        private final Map<ObservationAttributeSpec<?>, Object> attributes = new LinkedHashMap<>();
        private ObservationOutcome outcome = ObservationOutcome.UNSET;
        private @Nullable Throwable error;
        private boolean errorDispatched;
        private boolean stopped;
        private @Nullable Instant stoppedAt;
        private long stoppedNanos;

        private DefaultObservation(
                DefaultObservationRegistry registry,
                ObservationSpec spec,
                ObservationAttributeSet initialAttributes,
                @Nullable Observation parent,
                List<ObservationLink> links
        ) {
            this.registry = registry;
            this.spec = spec;
            this.parentId = parent instanceof DefaultObservation observation ? observation.id : null;
            this.startedAt = registry.clock.now();
            this.startedNanos = registry.clock.monotonicNanos();
            this.links = List.copyOf(links);
            this.attributes.putAll(initialAttributes.values());
        }

        @Override
        public ObservationSpec spec() {
            return spec;
        }

        @Override
        public synchronized ObservationOutcome outcome() {
            return outcome;
        }

        @Override
        public synchronized <T> void put(ObservationAttributeSpec<T> attribute, T value) {
            requireActive();
            ObservationAttributeSpec<T> nonNullAttribute = Objects.requireNonNull(attribute, "attribute");
            nonNullAttribute.formatValue(value);
            attributes.put(nonNullAttribute, Objects.requireNonNull(value, "value"));
        }

        @Override
        public void event(ObservationEventName eventName, ObservationAttributeSet eventAttributes) {
            ObservationEvent event;
            synchronized (this) {
                requireActive();
                event = new ObservationEvent(
                        Objects.requireNonNull(eventName, "eventName"),
                        registry.clock.now(),
                        Objects.requireNonNull(eventAttributes, "attributes")
                );
            }
            registry.dispatchEvent(view(), event);
        }

        @Override
        public void event(ObservationEventName eventName) {
            event(eventName, ObservationAttributeSet.empty());
        }

        @Override
        public void error(Throwable failure) {
            ObservationView snapshot = null;
            synchronized (this) {
                requireActive();
                if (failure == null || error != null) {
                    return;
                }
                error = failure;
                outcome = ObservationOutcome.ERROR;
                if (!errorDispatched) {
                    errorDispatched = true;
                    snapshot = view();
                }
            }
            if (snapshot != null) {
                registry.dispatchError(snapshot);
            }
        }

        @Override
        public synchronized void setOutcome(ObservationOutcome requestedOutcome) {
            requireActive();
            ObservationOutcome nonNullOutcome = Objects.requireNonNull(requestedOutcome, "outcome");
            if (outcome == ObservationOutcome.ERROR
                    && nonNullOutcome != ObservationOutcome.ERROR) {
                return;
            }
            outcome = nonNullOutcome;
        }

        @Override
        public ObservationScope openScope() {
            synchronized (this) {
                requireActive();
            }
            ExecutionContext current = ExecutionContexts.current().orElse(ExecutionContext.empty());
            ExecutionContext scopedContext;
            if (current.get(CURRENT_OBSERVATION).isPresent()) {
                scopedContext = current.replace(CURRENT_OBSERVATION, this);
            } else {
                scopedContext = current.with(CURRENT_OBSERVATION, this);
            }
            ExecutionContexts.Scope contextScope = ExecutionContexts.open(scopedContext);
            registry.dispatchScopeOpened(view());
            return new Scope(contextScope);
        }

        @Override
        public void stop() {
            ObservationView snapshot;
            synchronized (this) {
                if (stopped) {
                    return;
                }
                stopped = true;
                stoppedAt = registry.clock.now();
                stoppedNanos = registry.clock.monotonicNanos();
                snapshot = view();
            }
            registry.dispatchStop(snapshot);
        }

        @Override
        public void close() {
            stop();
        }

        private void dispatchStart() {
            registry.dispatchStart(view());
        }

        private synchronized ObservationView view() {
            Instant currentStoppedAt = stoppedAt;
            long currentNanos = stopped ? stoppedNanos : registry.clock.monotonicNanos();
            Duration elapsed = elapsed(startedNanos, currentNanos);
            return new SnapshotView(
                    id,
                    spec,
                    outcome,
                    ObservationAttributeSet.copyOf(attributes),
                    error,
                    parentId,
                    startedAt,
                    currentStoppedAt,
                    elapsed,
                    links
            );
        }

        private synchronized void requireActive() {
            if (stopped) {
                throw new IllegalStateException("Observation is already stopped");
            }
        }

        private final class Scope implements ObservationScope {
            private final ExecutionContexts.Scope contextScope;
            private boolean closed;

            private Scope(ExecutionContexts.Scope contextScope) {
                this.contextScope = contextScope;
            }

            @Override
            public synchronized void close() {
                if (closed) {
                    return;
                }
                contextScope.close();
                closed = true;
                registry.dispatchScopeClosed(view());
            }
        }
    }

    private static final class SnapshotView implements ObservationView {
        private final ObservationId id;
        private final ObservationSpec spec;
        private final ObservationOutcome outcome;
        private final ObservationAttributeSet attributes;
        private final @Nullable Throwable error;
        private final @Nullable ObservationId parentId;
        private final Instant startedAt;
        private final @Nullable Instant stoppedAt;
        private final Duration elapsed;
        private final List<ObservationLink> links;

        private SnapshotView(
                ObservationId id,
                ObservationSpec spec,
                ObservationOutcome outcome,
                ObservationAttributeSet attributes,
                @Nullable Throwable error,
                @Nullable ObservationId parentId,
                Instant startedAt,
                @Nullable Instant stoppedAt,
                Duration elapsed,
                List<ObservationLink> links
        ) {
            this.id = id;
            this.spec = spec;
            this.outcome = outcome;
            this.attributes = attributes;
            this.error = error;
            this.parentId = parentId;
            this.startedAt = startedAt;
            this.stoppedAt = stoppedAt;
            this.elapsed = elapsed;
            this.links = List.copyOf(links);
        }

        @Override
        public ObservationId id() {
            return id;
        }

        @Override
        public ObservationSpec spec() {
            return spec;
        }

        @Override
        public ObservationOutcome outcome() {
            return outcome;
        }

        @Override
        public ObservationAttributeSet attributes() {
            return attributes;
        }

        @Override
        public @Nullable Throwable error() {
            return error;
        }

        @Override
        public @Nullable ObservationId parentId() {
            return parentId;
        }

        @Override
        public Instant startedAt() {
            return startedAt;
        }

        @Override
        public @Nullable Instant stoppedAt() {
            return stoppedAt;
        }

        @Override
        public Duration elapsed() {
            return elapsed;
        }

        @Override
        public List<ObservationLink> links() {
            return links;
        }
    }

    private void dispatchStart(ObservationView view) {
        for (ObservationHandler handler : handlers) {
            invoke(() -> handler.onStart(view));
        }
    }

    private void dispatchError(ObservationView view) {
        for (ObservationHandler handler : handlers) {
            invoke(() -> handler.onError(view));
        }
    }

    private void dispatchEvent(ObservationView view, ObservationEvent event) {
        for (ObservationHandler handler : handlers) {
            invoke(() -> handler.onEvent(view, event));
        }
    }

    private void dispatchScopeOpened(ObservationView view) {
        for (ObservationHandler handler : handlers) {
            invoke(() -> handler.onScopeOpened(view));
        }
    }

    private void dispatchScopeClosed(ObservationView view) {
        for (ObservationHandler handler : handlers) {
            invoke(() -> handler.onScopeClosed(view));
        }
    }

    private void dispatchStop(ObservationView view) {
        for (ObservationHandler handler : handlers) {
            invoke(() -> handler.onStop(view));
        }
    }

    private static void invoke(Runnable callback) {
        try {
            callback.run();
        } catch (Throwable ignored) {
            // Handler failures are isolated from the observed operation.
        }
    }

    private static Duration elapsed(long startedNanos, long endedNanos) {
        long nanos;
        try {
            nanos = Math.subtractExact(endedNanos, startedNanos);
        } catch (ArithmeticException failure) {
            nanos = Long.MAX_VALUE;
        }
        if (nanos < 0L) {
            throw new IllegalStateException("Observability clock moved backwards");
        }
        return Duration.ofNanos(nanos);
    }
}
