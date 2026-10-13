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

import com.lamprism.luxspec.context.ThreadLocalExecutionContextStorage;
import com.lamprism.luxspec.observability.observation.Observation;
import com.lamprism.luxspec.observability.observation.ObservationAttributeClassification;
import com.lamprism.luxspec.observability.observation.ObservationAttributeSet;
import com.lamprism.luxspec.observability.observation.ObservationAttributeSpec;
import com.lamprism.luxspec.observability.observation.ObservationEventName;
import com.lamprism.luxspec.observability.observation.ObservationKind;
import com.lamprism.luxspec.observability.observation.ObservationLink;
import com.lamprism.luxspec.observability.observation.ObservationOutcome;
import com.lamprism.luxspec.observability.observation.ObservationRegistry;
import com.lamprism.luxspec.observability.observation.ObservationScope;
import com.lamprism.luxspec.observability.observation.ObservationSpec;
import com.lamprism.luxspec.observability.observation.ObservationStart;
import com.lamprism.luxspec.observability.runtime.observation.ObservationRegistryBuilder;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OtlpObservationBridgeTest {
    private static final ObservationAttributeSpec<String> OPERATION = ObservationAttributeSpec.string(
            "operation.name",
            ObservationAttributeClassification.LOW
    );

    @Test
    void projectsLuxspecLifecycleIntoProviderSpans() {
        RecordingSpanExporter exporter = new RecordingSpanExporter();
        SdkTracerProvider provider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(provider)
                .build();
        ObservationSpec spec = ObservationSpec.builder("test.request")
                .kind(ObservationKind.SERVER)
                .build();
        ObservationLink link = ObservationLink.of(
                "11111111111111111111111111111111",
                "2222222222222222",
                Map.of("source", "test")
        );

        try (ObservationRegistry registry = ObservationRegistryBuilder.builder()
                .contextStorage(new ThreadLocalExecutionContextStorage())
                .build();
             OtlpObservationBridge ignored = new OtlpObservationBridge(
                     registry,
                     openTelemetry,
                     "luxspec-test"
             )) {
            registry.register(spec);
            Observation parent = registry.start(ObservationStart.builder(spec)
                    .attributes(ObservationAttributeSet.builder().put(OPERATION, "parent").build())
                    .link(link)
                    .build());
            try (ObservationScope ignoredScope = parent.openScope()) {
                Observation child = registry.start(spec);
                child.put(OPERATION, "child");
                child.event(ObservationEventName.of("child.event"));
                child.error(new IllegalStateException("expected test failure"));
                child.stop();
            }
            parent.setOutcome(ObservationOutcome.SUCCESS);
            parent.stop();
        } finally {
            provider.close();
        }

        assertEquals(2, exporter.spans.size());
        SpanData parent = exporter.find("test.request", "parent");
        SpanData child = exporter.find("test.request", "child");
        assertEquals(SpanKind.SERVER, parent.getKind());
        assertEquals("parent", parent.getAttributes().get(AttributeKey.stringKey(OPERATION.name())));
        assertEquals(1, parent.getLinks().size());
        assertEquals(2, child.getEvents().size());
        assertEquals(
                parent.getSpanContext().getSpanId(),
                child.getParentSpanContext().getSpanId()
        );
        assertEquals(StatusCode.ERROR, child.getStatus().getStatusCode());
        assertFalse(parent.getStatus().getStatusCode() == StatusCode.ERROR);
    }

    private static final class RecordingSpanExporter implements SpanExporter {
        private final List<SpanData> spans = new ArrayList<>();

        @Override
        public CompletableResultCode export(Collection<SpanData> spans) {
            this.spans.addAll(spans);
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode flush() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            return CompletableResultCode.ofSuccess();
        }

        private SpanData find(String name, String operation) {
            return spans.stream()
                    .filter(span -> name.equals(span.getName()))
                    .filter(span -> operation.equals(
                            span.getAttributes().get(AttributeKey.stringKey(OPERATION.name()))
                    ))
                    .findFirst()
                    .orElseThrow();
        }
    }
}
