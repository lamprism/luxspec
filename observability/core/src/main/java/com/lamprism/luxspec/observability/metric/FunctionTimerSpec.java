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

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Declares a value-backed function timer.
 *
 * @author RollW
 */
public final class FunctionTimerSpec<S> extends AbstractMetricSpec<FunctionTimer> {
    private final Class<S> sourceType;
    private final LongReader<? super S> countReader;
    private final DurationReader<? super S> totalTimeReader;

    private FunctionTimerSpec(Builder<S> builder) {
        super(builder.name(), builder.kind(), builder.dimensions(), builder.description(), builder.baseUnit(), builder.cardinalityPolicy());
        sourceType = builder.sourceType;
        countReader = Objects.requireNonNull(builder.countReader, "countReader");
        totalTimeReader = Objects.requireNonNull(builder.totalTimeReader, "totalTimeReader");
    }

    public static <S> Builder<S> builder(String name, Class<S> sourceType) {
        return new Builder<>(name, sourceType);
    }

    public Class<S> sourceType() {
        return sourceType;
    }

    public LongReader<? super S> countReader() {
        return countReader;
    }

    public DurationReader<? super S> totalTimeReader() {
        return totalTimeReader;
    }

    public MetricBinding<FunctionTimer> bind(MetricDimensionSet dimensions, S source) {
        return bindValueSource(dimensions, source);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof FunctionTimerSpec<?> spec) || !super.equals(other)) {
            return false;
        }
        return sourceType.equals(spec.sourceType)
                && countReader.getClass().equals(spec.countReader.getClass())
                && totalTimeReader.getClass().equals(spec.totalTimeReader.getClass());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sourceType, countReader.getClass(), totalTimeReader.getClass());
    }

    /**
     * Builds a function timer specification.
     */
    public static final class Builder<S> extends BuilderSupport<FunctionTimer, Builder<S>> {
        private final Class<S> sourceType;
        private @Nullable LongReader<? super S> countReader;
        private @Nullable DurationReader<? super S> totalTimeReader;

        private Builder(String name, Class<S> sourceType) {
            super(name, MetricKind.FUNCTION_TIMER);
            this.sourceType = Objects.requireNonNull(sourceType, "sourceType");
            if (sourceType.isPrimitive()) {
                throw new IllegalArgumentException("Metric value source types must use boxed classes");
            }
        }

        public Builder<S> count(LongReader<? super S> countReader) {
            this.countReader = Objects.requireNonNull(countReader, "countReader");
            return this;
        }

        public Builder<S> totalTime(DurationReader<? super S> totalTimeReader) {
            this.totalTimeReader = Objects.requireNonNull(totalTimeReader, "totalTimeReader");
            return this;
        }

        public FunctionTimerSpec<S> build() {
            if (countReader == null || totalTimeReader == null) {
                throw new IllegalStateException("Function timer count and total-time readers are required");
            }
            return new FunctionTimerSpec<>(this);
        }
    }
}
