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

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

import java.util.Objects;

/**
 * Owns an explicit OTLP gRPC telemetry provider for applications that do not already configure
 * OpenTelemetry.
 *
 * @author RollW
 */
public final class OtlpTelemetry implements AutoCloseable {
    private final OpenTelemetrySdk openTelemetry;
    private final SdkTracerProvider tracerProvider;

    private OtlpTelemetry(OpenTelemetrySdk openTelemetry, SdkTracerProvider tracerProvider) {
        this.openTelemetry = openTelemetry;
        this.tracerProvider = tracerProvider;
    }

    /**
     * Creates an OTLP gRPC provider using an explicit collector endpoint.
     *
     * @param endpoint the OTLP gRPC endpoint
     * @return the owned telemetry provider
     */
    public static OtlpTelemetry grpc(String endpoint) {
        String nonNullEndpoint = Objects.requireNonNull(endpoint, "endpoint");
        if (nonNullEndpoint.isBlank()) {
            throw new IllegalArgumentException("endpoint must not be blank");
        }
        OtlpGrpcSpanExporter exporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint(nonNullEndpoint)
                .build();
        SdkTracerProvider provider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        OpenTelemetrySdk telemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(provider)
                .build();
        return new OtlpTelemetry(telemetry, provider);
    }

    /**
     * Returns the configured OpenTelemetry provider.
     *
     * @return the provider
     */
    public OpenTelemetry openTelemetry() {
        return openTelemetry;
    }

    @Override
    public void close() {
        tracerProvider.close();
    }
}
