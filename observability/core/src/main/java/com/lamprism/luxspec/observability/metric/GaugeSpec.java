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

package com.lamprism.luxspec.observability.metric;

import java.util.Objects;

/**
 * Declares a value-backed numeric gauge.
 *
 * @author RollW
 */
public final class GaugeSpec<S> extends AbstractMetricSpec<Gauge> {
    private final Class<S> sourceType;
    private final NumberReader<? super S> reader;

    private GaugeSpec(Builder<S> builder) {
        super(builder.name(), builder.kind(), builder.dimensions(), builder.description(), builder.baseUnit(), builder.cardinalityPolicy());
        sourceType = builder.sourceType;
        reader = builder.reader;
    }

    public static <S> Builder<S> builder(String name, Class<S> sourceType) {
        return new Builder<>(name, sourceType);
    }

    public Class<S> sourceType() {
        return sourceType;
    }

    public NumberReader<? super S> reader() {
        return reader;
    }

    public MetricBinding<Gauge> bind(MetricDimensionSet dimensions, S source) {
        return bindValueSource(dimensions, source);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof GaugeSpec<?> spec) || !super.equals(other)) {
            return false;
        }
        return sourceType.equals(spec.sourceType)
                && reader.getClass().equals(spec.reader.getClass());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sourceType, reader.getClass());
    }

    /**
     * Builds a gauge specification.
     */
    public static final class Builder<S> extends BuilderSupport<Gauge, Builder<S>> {
        private final Class<S> sourceType;
        private NumberReader<? super S> reader;

        private Builder(String name, Class<S> sourceType) {
            super(name, MetricKind.GAUGE);
            this.sourceType = Objects.requireNonNull(sourceType, "sourceType");
            if (sourceType.isPrimitive()) {
                throw new IllegalArgumentException("Metric value source types must use boxed classes");
            }
        }

        public Builder<S> reader(NumberReader<? super S> reader) {
            this.reader = Objects.requireNonNull(reader, "reader");
            return this;
        }

        public GaugeSpec<S> build() {
            if (reader == null) {
                throw new IllegalStateException("A gauge reader is required");
            }
            return new GaugeSpec<>(this);
        }
    }
}
