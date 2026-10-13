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

package com.lamprism.luxspec.observability.otlp;

import com.lamprism.luxspec.observability.observation.ObservationAttributeSet;
import com.lamprism.luxspec.observability.observation.ObservationAttributeSpec;
import com.lamprism.luxspec.observability.observation.ObservationEvent;
import com.lamprism.luxspec.observability.observation.ObservationHandler;
import com.lamprism.luxspec.observability.observation.ObservationId;
import com.lamprism.luxspec.observability.observation.ObservationKind;
import com.lamprism.luxspec.observability.observation.ObservationLink;
import com.lamprism.luxspec.observability.observation.ObservationOutcome;
import com.lamprism.luxspec.observability.observation.ObservationRegistry;
import com.lamprism.luxspec.observability.observation.ObservationView;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Projects Luxspec observations into OpenTelemetry spans.
 *
 * <p>The adapter accepts an application-owned {@link OpenTelemetry} instance. Exporter selection,
 * resource attributes, sampling, and shutdown ownership remain with the OpenTelemetry adapter or
 * application.</p>
 *
 * @author RollW
 */
public final class OtlpObservationBridge implements AutoCloseable {
    private final Tracer tracer;
    private final ObservationRegistry registry;
    private final ConcurrentMap<ObservationId, ProviderSpan> spans = new ConcurrentHashMap<>();
    private final ObservationHandler handler = new BridgeHandler();
    private final AutoCloseable listener;
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * Creates an OpenTelemetry observation bridge.
     *
     * @param observationRegistry the Luxspec observation registry
     * @param openTelemetry       the application-owned OpenTelemetry provider
     * @param instrumentationName the tracer instrumentation name
     */
    public OtlpObservationBridge(
            ObservationRegistry observationRegistry,
            OpenTelemetry openTelemetry,
            String instrumentationName
    ) {
        registry = Objects.requireNonNull(observationRegistry, "observationRegistry");
        OpenTelemetry nonNullOpenTelemetry = Objects.requireNonNull(openTelemetry, "openTelemetry");
        String name = Objects.requireNonNull(instrumentationName, "instrumentationName");
        if (name.isBlank()) {
            throw new IllegalArgumentException("instrumentationName must not be blank");
        }
        tracer = nonNullOpenTelemetry.getTracer(name);
        listener = registry.addHandler(handler);
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        try {
            listener.close();
        } catch (Exception failure) {
            throw new OtlpProjectionException("Unable to detach the OpenTelemetry observation bridge", failure);
        } finally {
            for (ProviderSpan span : spans.values()) {
                span.close();
            }
            spans.clear();
        }
    }

    private final class BridgeHandler implements ObservationHandler {
        @Override
        public void onStart(ObservationView view) {
            if (closed.get()) {
                return;
            }
            SpanBuilder builder = tracer.spanBuilder(view.spec().getName().value())
                    .setSpanKind(spanKind(view.spec().getKind()))
                    .setStartTimestamp(view.startedAt().toEpochMilli(), TimeUnit.MILLISECONDS);
            ProviderSpan parent = view.parentId() == null ? null : spans.get(view.parentId());
            if (parent != null) {
                builder.setParent(parent.span().storeInContext(Context.current()));
            }
            for (ObservationLink link : view.links()) {
                SpanContext context = linkContext(link);
                if (context.isValid()) {
                    builder.addLink(context, attributes(link.attributes()));
                }
            }
            applyAttributes(builder, view.attributes());
            spans.put(view.id(), new ProviderSpan(builder.startSpan()));
        }

        @Override
        public void onError(ObservationView view) {
            ProviderSpan providerSpan = spans.get(view.id());
            if (providerSpan == null || view.error() == null) {
                return;
            }
            providerSpan.span().recordException(view.error());
            providerSpan.span().setStatus(StatusCode.ERROR);
        }

        @Override
        public void onEvent(ObservationView view, ObservationEvent event) {
            ProviderSpan providerSpan = spans.get(view.id());
            if (providerSpan == null) {
                return;
            }
            providerSpan.span().addEvent(
                    event.name().value(),
                    attributes(event.attributes()),
                    event.occurredAt().toEpochMilli(),
                    TimeUnit.MILLISECONDS
            );
        }

        @Override
        public void onScopeOpened(ObservationView view) {
            ProviderSpan providerSpan = spans.get(view.id());
            if (providerSpan != null) {
                providerSpan.openScope();
            }
        }

        @Override
        public void onScopeClosed(ObservationView view) {
            ProviderSpan providerSpan = spans.get(view.id());
            if (providerSpan != null) {
                providerSpan.closeScope();
                if (providerSpan.isStopped() && providerSpan.isScopeFree()) {
                    spans.remove(view.id(), providerSpan);
                }
            }
        }

        @Override
        public void onStop(ObservationView view) {
            ProviderSpan providerSpan = spans.get(view.id());
            if (providerSpan == null) {
                return;
            }
            applyAttributes(providerSpan.span(), view.attributes());
            if (view.outcome() == ObservationOutcome.ERROR) {
                providerSpan.span().setStatus(StatusCode.ERROR);
            } else if (view.outcome() == ObservationOutcome.SUCCESS) {
                providerSpan.span().setStatus(StatusCode.OK);
            }
            Instant stoppedAt = view.stoppedAt();
            if (stoppedAt == null) {
                providerSpan.span().end();
            } else {
                providerSpan.span().end(stoppedAt.toEpochMilli(), TimeUnit.MILLISECONDS);
            }
            providerSpan.markStopped();
            if (providerSpan.isScopeFree()) {
                spans.remove(view.id(), providerSpan);
            }
        }
    }

    private static SpanKind spanKind(ObservationKind kind) {
        return switch (kind) {
            case SERVER -> SpanKind.SERVER;
            case CLIENT -> SpanKind.CLIENT;
            case PRODUCER -> SpanKind.PRODUCER;
            case CONSUMER -> SpanKind.CONSUMER;
            case INTERNAL -> SpanKind.INTERNAL;
        };
    }

    private static void applyAttributes(SpanBuilder builder, ObservationAttributeSet attributes) {
        for (Map.Entry<ObservationAttributeSpec<?>, Object> entry : attributes.values().entrySet()) {
            ObservationAttributeSpec<?> spec = entry.getKey();
            builder.setAttribute(spec.name(), format(spec, entry.getValue()));
        }
    }

    private static void applyAttributes(Span span, ObservationAttributeSet attributes) {
        for (Map.Entry<ObservationAttributeSpec<?>, Object> entry : attributes.values().entrySet()) {
            ObservationAttributeSpec<?> spec = entry.getKey();
            span.setAttribute(spec.name(), format(spec, entry.getValue()));
        }
    }

    private static Attributes attributes(Map<String, String> values) {
        AttributesBuilder builder = Attributes.builder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            builder.put(AttributeKey.stringKey(entry.getKey()), entry.getValue());
        }
        return builder.build();
    }

    private static Attributes attributes(ObservationAttributeSet values) {
        AttributesBuilder builder = Attributes.builder();
        for (Map.Entry<ObservationAttributeSpec<?>, Object> entry : values.values().entrySet()) {
            ObservationAttributeSpec<?> spec = entry.getKey();
            builder.put(spec.name(), format(spec, entry.getValue()));
        }
        return builder.build();
    }

    private static SpanContext linkContext(ObservationLink link) {
        return SpanContext.createFromRemoteParent(
                link.traceId(),
                link.spanId(),
                TraceFlags.getSampled(),
                TraceState.getDefault()
        );
    }

    private static String format(ObservationAttributeSpec<?> spec, Object value) {
        return formatUnchecked(spec, value);
    }

    private static <T> String formatUnchecked(ObservationAttributeSpec<T> spec, Object value) {
        return spec.formatValue(spec.valueType().cast(value));
    }

    private static final class ProviderSpan {
        private final Span span;
        private final ConcurrentLinkedDeque<Scope> scopes = new ConcurrentLinkedDeque<>();
        private final AtomicBoolean stopped = new AtomicBoolean();

        private ProviderSpan(Span span) {
            this.span = Objects.requireNonNull(span, "span");
        }

        private Span span() {
            return span;
        }

        private void openScope() {
            if (!stopped.get()) {
                scopes.push(span.makeCurrent());
            }
        }

        private void closeScope() {
            Scope scope = scopes.pollFirst();
            if (scope != null) {
                scope.close();
            }
        }

        private void markStopped() {
            stopped.set(true);
        }

        private boolean isStopped() {
            return stopped.get();
        }

        private boolean isScopeFree() {
            return scopes.isEmpty();
        }

        private void close() {
            Scope scope;
            while ((scope = scopes.pollFirst()) != null) {
                scope.close();
            }
            if (stopped.compareAndSet(false, true)) {
                span.end();
            }
        }
    }
}
