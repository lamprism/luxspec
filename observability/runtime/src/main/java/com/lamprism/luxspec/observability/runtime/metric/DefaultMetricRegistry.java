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

package com.lamprism.luxspec.observability.runtime.metric;

import com.lamprism.luxspec.observability.ObservabilityClock;
import com.lamprism.luxspec.observability.metric.CardinalityOverflow;
import com.lamprism.luxspec.observability.metric.Counter;
import com.lamprism.luxspec.observability.metric.CounterSpec;
import com.lamprism.luxspec.observability.metric.DistributionSpec;
import com.lamprism.luxspec.observability.metric.DistributionSummary;
import com.lamprism.luxspec.observability.metric.DistributionSummarySpec;
import com.lamprism.luxspec.observability.metric.DurationReader;
import com.lamprism.luxspec.observability.metric.FunctionCounter;
import com.lamprism.luxspec.observability.metric.FunctionCounterSpec;
import com.lamprism.luxspec.observability.metric.FunctionTimer;
import com.lamprism.luxspec.observability.metric.FunctionTimerSpec;
import com.lamprism.luxspec.observability.metric.Gauge;
import com.lamprism.luxspec.observability.metric.GaugeSpec;
import com.lamprism.luxspec.observability.metric.LongReader;
import com.lamprism.luxspec.observability.metric.LongTaskTimer;
import com.lamprism.luxspec.observability.metric.LongTaskTimerSpec;
import com.lamprism.luxspec.observability.metric.LongTaskTiming;
import com.lamprism.luxspec.observability.metric.Metric;
import com.lamprism.luxspec.observability.metric.MetricActivation;
import com.lamprism.luxspec.observability.metric.MetricBinding;
import com.lamprism.luxspec.observability.metric.MetricCardinalityException;
import com.lamprism.luxspec.observability.metric.MetricCardinalityPolicy;
import com.lamprism.luxspec.observability.metric.MetricKind;
import com.lamprism.luxspec.observability.metric.MetricName;
import com.lamprism.luxspec.observability.metric.MetricReading;
import com.lamprism.luxspec.observability.metric.MetricRegistry;
import com.lamprism.luxspec.observability.metric.MetricSnapshot;
import com.lamprism.luxspec.observability.metric.MetricSpec;
import com.lamprism.luxspec.observability.metric.NumberReader;
import com.lamprism.luxspec.observability.metric.TimeGauge;
import com.lamprism.luxspec.observability.metric.TimeGaugeSpec;
import com.lamprism.luxspec.observability.metric.Timer;
import com.lamprism.luxspec.observability.metric.TimerSpec;
import com.lamprism.luxspec.observability.metric.TimerTiming;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Collects and materializes registered metric bindings.
 *
 * @author RollW
 */
final class DefaultMetricRegistry implements MetricRegistry, MetricRegistryEventSource {
    private final ObservabilityClock clock;
    private final MetricActivation activation;
    private final Map<MetricName, MetricSpec<?>> specs = new LinkedHashMap<>();
    private final Map<MetricBinding<?>, Metric> metrics = new LinkedHashMap<>();
    private final Map<MetricName, Integer> bindingCounts = new LinkedHashMap<>();
    private final CopyOnWriteArrayList<Consumer<MetricSpec<?>>> registrationListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Metric>> materializationListeners = new CopyOnWriteArrayList<>();
    private long sequence;
    private boolean closed;

    DefaultMetricRegistry(ObservabilityClock clock, MetricActivation activation) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.activation = Objects.requireNonNull(activation, "activation");
    }

    @Override
    public synchronized void register(MetricSpec<?> spec) {
        requireOpen();
        MetricSpec<?> nonNullSpec = Objects.requireNonNull(spec, "spec");
        MetricSpec<?> previous = specs.get(nonNullSpec.getName());
        if (previous != null) {
            if (!previous.equals(nonNullSpec)) {
                throw new IllegalArgumentException("Conflicting metric definition: " + nonNullSpec.getName());
            }
            return;
        }
        specs.put(nonNullSpec.getName(), nonNullSpec);
        for (Consumer<MetricSpec<?>> listener : registrationListeners) {
            try {
                listener.accept(nonNullSpec);
            } catch (RuntimeException ignored) {
                // Provider adapters must report their own projection failures.
            }
        }
    }

    @Override
    public synchronized @Nullable MetricSpec<?> find(MetricName name) {
        requireOpen();
        return specs.get(Objects.requireNonNull(name, "name"));
    }

    @Override
    public synchronized List<MetricSpec<?>> getSpecs() {
        requireOpen();
        return List.copyOf(specs.values());
    }

    @Override
    public synchronized <M extends Metric> M obtain(MetricBinding<M> binding) {
        requireOpen();
        MetricBinding<M> nonNullBinding = Objects.requireNonNull(binding, "binding");
        MetricSpec<?> registered = specs.get(nonNullBinding.getSpec().getName());
        if (registered == null || !registered.equals(nonNullBinding.getSpec())) {
            throw new IllegalStateException("Metric spec is not registered: " + nonNullBinding.getSpec().getName());
        }
        Metric existing = metrics.get(nonNullBinding.withoutValueSource());
        if (existing != null) {
            return cast(existing);
        }
        if (!activation.isEnabled(registered)) {
            return cast(noOp(nonNullBinding.withoutValueSource()));
        }
        if (!reserveBinding(registered)) {
            MetricCardinalityPolicy policy = Objects.requireNonNull(
                    registered.getCardinalityPolicy(),
                    "cardinalityPolicy"
            );
            if (policy.overflow() == CardinalityOverflow.REJECT) {
                throw new MetricCardinalityException(registered.getName(), policy.maximumBindings());
            }
            return cast(noOp(nonNullBinding.withoutValueSource()));
        }
        Metric created = createMetric(nonNullBinding);
        metrics.put(nonNullBinding.withoutValueSource(), created);
        for (Consumer<Metric> listener : materializationListeners) {
            listener.accept(created);
        }
        return cast(created);
    }

    @Override
    public synchronized <M extends Metric> M obtain(MetricSpec<M> spec) {
        MetricSpec<M> nonNullSpec = Objects.requireNonNull(spec, "spec");
        if (!nonNullSpec.getDimensions().isEmpty()) {
            throw new IllegalArgumentException("A dimensioned metric requires a MetricDimensionSet");
        }
        return obtain(nonNullSpec.bind());
    }

    @Override
    public synchronized @Nullable <M extends Metric> M find(MetricBinding<M> binding) {
        requireOpen();
        Metric metric = metrics.get(Objects.requireNonNull(binding, "binding").withoutValueSource());
        if (metric == null) {
            return null;
        }
        return cast(metric);
    }

    @Override
    public synchronized MetricSnapshot snapshot() {
        requireOpen();
        List<MetricReading> readings = new ArrayList<>();
        for (Metric metric : metrics.values()) {
            if (metric instanceof ReadableMetric readable) {
                MetricReading reading = readable.read();
                if (reading != null) {
                    readings.add(reading);
                }
            }
        }
        return new MetricSnapshot(clock.now(), ++sequence, readings);
    }

    @Override
    public synchronized AutoCloseable addRegistrationListener(Consumer<MetricSpec<?>> listener) {
        requireOpen();
        Consumer<MetricSpec<?>> nonNullListener = Objects.requireNonNull(listener, "listener");
        registrationListeners.add(nonNullListener);
        return () -> registrationListeners.remove(nonNullListener);
    }

    @Override
    public synchronized AutoCloseable addMaterializationListener(Consumer<Metric> listener) {
        requireOpen();
        Consumer<Metric> nonNullListener = Objects.requireNonNull(listener, "listener");
        materializationListeners.add(nonNullListener);
        try {
            for (Metric metric : metrics.values()) {
                nonNullListener.accept(metric);
            }
        } catch (RuntimeException failure) {
            materializationListeners.remove(nonNullListener);
            throw failure;
        }
        return () -> materializationListeners.remove(nonNullListener);
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        metrics.clear();
        specs.clear();
        bindingCounts.clear();
        registrationListeners.clear();
        materializationListeners.clear();
    }

    boolean isOpen() {
        synchronized (this) {
            return !closed;
        }
    }

    ObservabilityClock clock() {
        return clock;
    }

    private boolean reserveBinding(MetricSpec<?> spec) {
        MetricCardinalityPolicy policy = spec.getCardinalityPolicy();
        if (policy == null) {
            return true;
        }
        int current = bindingCounts.getOrDefault(spec.getName(), 0);
        if (current >= policy.maximumBindings()) {
            return false;
        }
        bindingCounts.put(spec.getName(), current + 1);
        return true;
    }

    private Metric createMetric(MetricBinding<?> binding) {
        MetricSpec<?> spec = binding.getSpec();
        if (spec instanceof CounterSpec) {
            return new CounterMetric(binding);
        }
        if (spec instanceof GaugeSpec<?> gaugeSpec) {
            return new GaugeMetric(binding, gaugeSpec);
        }
        if (spec instanceof FunctionCounterSpec<?> functionCounterSpec) {
            return new FunctionCounterMetric(binding, functionCounterSpec);
        }
        if (spec instanceof TimerSpec timerSpec) {
            return new TimerMetric(binding, timerSpec.distribution());
        }
        if (spec instanceof DistributionSummarySpec summarySpec) {
            return new DistributionSummaryMetric(binding, summarySpec.distribution(), summarySpec.nonNegative());
        }
        if (spec instanceof LongTaskTimerSpec) {
            return new LongTaskTimerMetric(binding);
        }
        if (spec instanceof FunctionTimerSpec<?> functionTimerSpec) {
            return new FunctionTimerMetric(binding, functionTimerSpec);
        }
        if (spec instanceof TimeGaugeSpec<?> timeGaugeSpec) {
            return new TimeGaugeMetric(binding, timeGaugeSpec);
        }
        throw new IllegalArgumentException("Unsupported metric spec: " + spec.getClass().getName());
    }

    private static Metric noOp(MetricBinding<?> binding) {
        MetricSpec<?> spec = binding.getSpec();
        if (spec instanceof CounterSpec) {
            return new NoOpCounter(binding);
        }
        if (spec instanceof GaugeSpec<?>) {
            return new NoOpGauge(binding);
        }
        if (spec instanceof FunctionCounterSpec<?>) {
            return new NoOpFunctionCounter(binding);
        }
        if (spec instanceof TimerSpec) {
            return new NoOpTimer(binding);
        }
        if (spec instanceof DistributionSummarySpec) {
            return new NoOpDistributionSummary(binding);
        }
        if (spec instanceof LongTaskTimerSpec) {
            return new NoOpLongTaskTimer(binding);
        }
        if (spec instanceof FunctionTimerSpec<?>) {
            return new NoOpFunctionTimer(binding);
        }
        if (spec instanceof TimeGaugeSpec<?>) {
            return new NoOpTimeGauge(binding);
        }
        throw new IllegalArgumentException("Unsupported metric spec: " + spec.getClass().getName());
    }

    @SuppressWarnings("unchecked")
    private static <M extends Metric> M cast(Metric metric) {
        return (M) metric;
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Metric registry is closed");
        }
    }

    private interface ReadableMetric {
        @Nullable
        MetricReading read();
    }

    private abstract class AbstractMetric implements Metric, ReadableMetric {
        private final MetricBinding<?> binding;

        private AbstractMetric(MetricBinding<?> binding) {
            this.binding = binding;
        }

        @Override
        public MetricBinding<?> getBinding() {
            return binding;
        }

        protected boolean registryOpen() {
            return isOpen();
        }
    }

    private final class CounterMetric extends AbstractMetric implements Counter {
        private double count;

        private CounterMetric(MetricBinding<?> binding) {
            super(binding);
        }

        @Override
        public synchronized void increment() {
            add(1.0d);
        }

        @Override
        public synchronized void add(double amount) {
            requireFiniteNonNegative(amount, "Counter increment");
            if (amount == 0.0d) {
                return;
            }
            double updated = count + amount;
            if (!Double.isFinite(updated) || updated < count) {
                throw new ArithmeticException("Counter value overflowed");
            }
            count = updated;
        }

        @Override
        public synchronized double count() {
            return count;
        }

        @Override
        public synchronized MetricReading read() {
            return MetricReading.builder(getBinding(), MetricKind.COUNTER).value(count).build();
        }
    }

    private final class GaugeMetric extends AbstractMetric implements Gauge {
        private final GaugeSpec<?> spec;

        private GaugeMetric(MetricBinding<?> binding, GaugeSpec<?> spec) {
            super(binding);
            this.spec = spec;
        }

        @Override
        public @Nullable Double value() {
            return readNumber(spec.reader(), getBinding().getValueSource(), spec.sourceType(), false);
        }

        @Override
        public @Nullable MetricReading read() {
            Double value = value();
            if (value == null) {
                return null;
            }
            return MetricReading.builder(getBinding(), MetricKind.GAUGE).value(value).build();
        }
    }

    private final class FunctionCounterMetric extends AbstractMetric implements FunctionCounter {
        private final FunctionCounterSpec<?> spec;

        private FunctionCounterMetric(MetricBinding<?> binding, FunctionCounterSpec<?> spec) {
            super(binding);
            this.spec = spec;
        }

        @Override
        public @Nullable Double count() {
            return readNumber(spec.reader(), getBinding().getValueSource(), spec.sourceType(), true);
        }

        @Override
        public @Nullable MetricReading read() {
            Double value = count();
            if (value == null) {
                return null;
            }
            return MetricReading.builder(getBinding(), MetricKind.FUNCTION_COUNTER).value(value).build();
        }
    }

    private final class TimerMetric extends AbstractMetric implements Timer {
        private final DistributionSpec distribution;
        private long count;
        private Duration total = Duration.ZERO;
        private @Nullable Duration max;
        private final Map<Double, Long> histogram = new LinkedHashMap<>();

        private TimerMetric(MetricBinding<?> binding, DistributionSpec distribution) {
            super(binding);
            this.distribution = distribution;
        }

        @Override
        public synchronized void record(Duration duration) {
            Duration nonNullDuration = requireDuration(duration);
            long updatedCount;
            Duration updatedTotal;
            try {
                updatedCount = Math.addExact(count, 1L);
                updatedTotal = total.plus(nonNullDuration);
            } catch (ArithmeticException failure) {
                throw new ArithmeticException("Timer value overflowed");
            }
            Map<Double, Long> updatedHistogram = updatedHistogram(seconds(nonNullDuration));
            count = updatedCount;
            total = updatedTotal;
            if (max == null || nonNullDuration.compareTo(max) > 0) {
                max = nonNullDuration;
            }
            histogram.clear();
            histogram.putAll(updatedHistogram);
        }

        @Override
        public TimerTiming start() {
            return new TimerTimingImpl(this, clock.monotonicNanos());
        }

        @Override
        public synchronized long count() {
            return count;
        }

        @Override
        public synchronized Duration totalTime() {
            return total;
        }

        @Override
        public synchronized @Nullable Duration max() {
            return max;
        }

        @Override
        public synchronized MetricReading read() {
            return MetricReading.builder(getBinding(), MetricKind.TIMER)
                    .count(count)
                    .totalTime(total)
                    .max(max == null ? null : seconds(max))
                    .histogram(histogram)
                    .build();
        }

        private Map<Double, Long> updatedHistogram(double value) {
            if (!distribution.histogram() || distribution.serviceLevelObjectives().isEmpty()) {
                return histogram;
            }
            Map<Double, Long> result = new LinkedHashMap<>(histogram);
            for (double boundary : distribution.serviceLevelObjectives()) {
                if (value <= boundary) {
                    long previous = result.getOrDefault(boundary, 0L);
                    result.put(boundary, Math.addExact(previous, 1L));
                }
            }
            return result;
        }

        private double seconds(Duration duration) {
            return duration.getSeconds() + duration.getNano() / 1_000_000_000.0d;
        }
    }

    private final class DistributionSummaryMetric extends AbstractMetric implements DistributionSummary {
        private final DistributionSpec distribution;
        private final boolean nonNegative;
        private long count;
        private double total;
        private @Nullable Double max;
        private final Map<Double, Long> histogram = new LinkedHashMap<>();

        private DistributionSummaryMetric(MetricBinding<?> binding, DistributionSpec distribution, boolean nonNegative) {
            super(binding);
            this.distribution = distribution;
            this.nonNegative = nonNegative;
        }

        @Override
        public synchronized void record(double amount) {
            if (!Double.isFinite(amount)) {
                throw new IllegalArgumentException("Distribution value must be finite");
            }
            if (nonNegative && amount < 0.0d) {
                throw new IllegalArgumentException("Distribution value must not be negative");
            }
            double updatedTotal = total + amount;
            if (!Double.isFinite(updatedTotal)) {
                throw new ArithmeticException("Distribution total overflowed");
            }
            long updatedCount;
            try {
                updatedCount = Math.addExact(count, 1L);
            } catch (ArithmeticException failure) {
                throw new ArithmeticException("Distribution count overflowed");
            }
            Map<Double, Long> updatedHistogram = updatedHistogram(amount);
            count = updatedCount;
            total = updatedTotal;
            if (max == null || amount > max) {
                max = amount;
            }
            histogram.clear();
            histogram.putAll(updatedHistogram);
        }

        @Override
        public synchronized long count() {
            return count;
        }

        @Override
        public synchronized double totalAmount() {
            return total;
        }

        @Override
        public synchronized @Nullable Double max() {
            return max;
        }

        @Override
        public synchronized MetricReading read() {
            return MetricReading.builder(getBinding(), MetricKind.DISTRIBUTION_SUMMARY)
                    .count(count)
                    .total(total)
                    .max(max)
                    .histogram(histogram)
                    .build();
        }

        private Map<Double, Long> updatedHistogram(double value) {
            if (!distribution.histogram() || distribution.serviceLevelObjectives().isEmpty()) {
                return histogram;
            }
            Map<Double, Long> result = new LinkedHashMap<>(histogram);
            for (double boundary : distribution.serviceLevelObjectives()) {
                if (value <= boundary) {
                    long previous = result.getOrDefault(boundary, 0L);
                    result.put(boundary, Math.addExact(previous, 1L));
                }
            }
            return result;
        }
    }

    private final class LongTaskTimerMetric extends AbstractMetric implements LongTaskTimer {
        private final Map<LongTaskTimingState, Boolean> active = new LinkedHashMap<>();

        private LongTaskTimerMetric(MetricBinding<?> binding) {
            super(binding);
        }

        @Override
        public synchronized LongTaskTiming start() {
            if (active.size() == Long.MAX_VALUE) {
                throw new ArithmeticException("Long-task active count overflowed");
            }
            LongTaskTimingState timing = new LongTaskTimingState(this, clock.monotonicNanos());
            if (!registryOpen()) {
                return timing;
            }
            active.put(timing, Boolean.TRUE);
            return timing;
        }

        @Override
        public synchronized long activeTasks() {
            return active.size();
        }

        @Override
        public synchronized Duration activeDuration() {
            Duration total = Duration.ZERO;
            long now = clock.monotonicNanos();
            for (LongTaskTimingState timing : active.keySet()) {
                total = addElapsed(total, timing.elapsed(now));
            }
            return total;
        }

        @Override
        public synchronized MetricReading read() {
            return MetricReading.builder(getBinding(), MetricKind.LONG_TASK_TIMER)
                    .activeTasks((long) active.size())
                    .activeDuration(activeDuration())
                    .build();
        }

        private synchronized void finish(LongTaskTimingState timing) {
            if (registryOpen()) {
                active.remove(timing);
            }
        }
    }

    private final class FunctionTimerMetric extends AbstractMetric implements FunctionTimer {
        private final FunctionTimerSpec<?> spec;

        private FunctionTimerMetric(MetricBinding<?> binding, FunctionTimerSpec<?> spec) {
            super(binding);
            this.spec = spec;
        }

        @Override
        public @Nullable Long count() {
            Object source = getBinding().getValueSource();
            if (source == null) {
                return null;
            }
            try {
                Long value = readLong(spec.countReader(), source, spec.sourceType());
                if (value == null || value < 0L) {
                    return null;
                }
                return value;
            } catch (RuntimeException failure) {
                return null;
            }
        }

        @Override
        public @Nullable Duration totalTime() {
            Object source = getBinding().getValueSource();
            if (source == null) {
                return null;
            }
            try {
                Duration value = readDuration(spec.totalTimeReader(), source, spec.sourceType());
                if (value == null || value.isNegative()) {
                    return null;
                }
                return value;
            } catch (RuntimeException failure) {
                return null;
            }
        }

        @Override
        public @Nullable MetricReading read() {
            Long count = count();
            Duration totalTime = totalTime();
            if (count == null || totalTime == null) {
                return null;
            }
            return MetricReading.builder(getBinding(), MetricKind.FUNCTION_TIMER)
                    .count(count)
                    .totalTime(totalTime)
                    .build();
        }
    }

    private final class TimeGaugeMetric extends AbstractMetric implements TimeGauge {
        private final TimeGaugeSpec<?> spec;

        private TimeGaugeMetric(MetricBinding<?> binding, TimeGaugeSpec<?> spec) {
            super(binding);
            this.spec = spec;
        }

        @Override
        public @Nullable Duration value() {
            Object source = getBinding().getValueSource();
            if (source == null) {
                return null;
            }
            try {
                Duration value = readDuration(spec.reader(), source, spec.sourceType());
                if (value == null || value.isNegative()) {
                    return null;
                }
                return value;
            } catch (RuntimeException failure) {
                return null;
            }
        }

        @Override
        public @Nullable MetricReading read() {
            Duration value = value();
            if (value == null) {
                return null;
            }
            return MetricReading.builder(getBinding(), MetricKind.TIME_GAUGE).totalTime(value).build();
        }
    }

    private final class TimerTimingImpl implements TimerTiming {
        private final TimerMetric timer;
        private final long startedAt;
        private boolean stopped;
        private @Nullable Duration elapsed;

        private TimerTimingImpl(TimerMetric timer, long startedAt) {
            this.timer = timer;
            this.startedAt = startedAt;
        }

        @Override
        public synchronized Duration stop() {
            if (stopped) {
                return Objects.requireNonNull(elapsed, "elapsed");
            }
            Duration measured = elapsedSince(startedAt);
            elapsed = measured;
            stopped = true;
            if (timer.registryOpen()) {
                timer.record(measured);
            }
            return measured;
        }
    }

    private final class LongTaskTimingState implements LongTaskTiming {
        private final LongTaskTimerMetric timer;
        private final long startedAt;
        private boolean stopped;
        private @Nullable Duration elapsed;

        private LongTaskTimingState(LongTaskTimerMetric timer, long startedAt) {
            this.timer = timer;
            this.startedAt = startedAt;
        }

        @Override
        public synchronized Duration stop() {
            if (stopped) {
                return Objects.requireNonNull(elapsed, "elapsed");
            }
            Duration measured = elapsedSince(startedAt);
            elapsed = measured;
            stopped = true;
            timer.finish(this);
            return measured;
        }

        private Duration elapsed(long now) {
            return durationBetween(startedAt, now);
        }
    }

    private static final class NoOpCounter extends NoOpMetric implements Counter {
        private NoOpCounter(MetricBinding<?> binding) {
            super(binding);
        }

        @Override
        public void increment() {
        }

        @Override
        public void add(double amount) {
        }

        @Override
        public double count() {
            return 0.0d;
        }
    }

    private static final class NoOpGauge extends NoOpMetric implements Gauge {
        private NoOpGauge(MetricBinding<?> binding) {
            super(binding);
        }

        @Override
        public @Nullable Double value() {
            return null;
        }
    }

    private static final class NoOpFunctionCounter extends NoOpMetric implements FunctionCounter {
        private NoOpFunctionCounter(MetricBinding<?> binding) {
            super(binding);
        }

        @Override
        public @Nullable Double count() {
            return null;
        }
    }

    private static final class NoOpTimer extends NoOpMetric implements Timer {
        private NoOpTimer(MetricBinding<?> binding) {
            super(binding);
        }

        @Override
        public void record(Duration duration) {
        }

        @Override
        public TimerTiming start() {
            return () -> Duration.ZERO;
        }

        @Override
        public long count() {
            return 0L;
        }

        @Override
        public Duration totalTime() {
            return Duration.ZERO;
        }

        @Override
        public @Nullable Duration max() {
            return null;
        }
    }

    private static final class NoOpDistributionSummary extends NoOpMetric implements DistributionSummary {
        private NoOpDistributionSummary(MetricBinding<?> binding) {
            super(binding);
        }

        @Override
        public void record(double amount) {
        }

        @Override
        public long count() {
            return 0L;
        }

        @Override
        public double totalAmount() {
            return 0.0d;
        }

        @Override
        public @Nullable Double max() {
            return null;
        }
    }

    private static final class NoOpLongTaskTimer extends NoOpMetric implements LongTaskTimer {
        private NoOpLongTaskTimer(MetricBinding<?> binding) {
            super(binding);
        }

        @Override
        public LongTaskTiming start() {
            return () -> Duration.ZERO;
        }

        @Override
        public long activeTasks() {
            return 0L;
        }

        @Override
        public Duration activeDuration() {
            return Duration.ZERO;
        }
    }

    private static final class NoOpFunctionTimer extends NoOpMetric implements FunctionTimer {
        private NoOpFunctionTimer(MetricBinding<?> binding) {
            super(binding);
        }

        @Override
        public @Nullable Long count() {
            return null;
        }

        @Override
        public @Nullable Duration totalTime() {
            return null;
        }
    }

    private static final class NoOpTimeGauge extends NoOpMetric implements TimeGauge {
        private NoOpTimeGauge(MetricBinding<?> binding) {
            super(binding);
        }

        @Override
        public @Nullable Duration value() {
            return null;
        }
    }

    private abstract static class NoOpMetric implements Metric {
        private final MetricBinding<?> binding;

        private NoOpMetric(MetricBinding<?> binding) {
            this.binding = binding;
        }

        @Override
        public MetricBinding<?> getBinding() {
            return binding;
        }
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Double readNumber(
            NumberReader<?> reader,
            @Nullable Object source,
            Class<?> sourceType,
            boolean nonNegative
    ) {
        if (source == null || !sourceType.isInstance(source)) {
            return null;
        }
        try {
            Number number = ((NumberReader<Object>) reader).read(source);
            if (number == null) {
                return null;
            }
            double value = number.doubleValue();
            if (!Double.isFinite(value) || nonNegative && value < 0.0d) {
                return null;
            }
            return value;
        } catch (RuntimeException failure) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Long readLong(LongReader<?> reader, Object source, Class<?> sourceType) {
        if (!sourceType.isInstance(source)) {
            return null;
        }
        return ((LongReader<Object>) reader).read(source);
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Duration readDuration(
            DurationReader<?> reader,
            Object source,
            Class<?> sourceType
    ) {
        if (!sourceType.isInstance(source)) {
            return null;
        }
        return ((DurationReader<Object>) reader).read(source);
    }

    private Duration elapsedSince(long startedAt) {
        return durationBetween(startedAt, clock.monotonicNanos());
    }

    private static Duration durationBetween(long startedAt, long endedAt) {
        long nanos;
        try {
            nanos = Math.subtractExact(endedAt, startedAt);
        } catch (ArithmeticException failure) {
            nanos = Long.MAX_VALUE;
        }
        if (nanos < 0L) {
            throw new IllegalStateException("Observability clock moved backwards");
        }
        return Duration.ofNanos(nanos);
    }

    private static Duration addElapsed(Duration first, Duration second) {
        try {
            return first.plus(second);
        } catch (ArithmeticException failure) {
            return Duration.ofNanos(Long.MAX_VALUE);
        }
    }

    private static Duration requireDuration(Duration duration) {
        Duration nonNullDuration = Objects.requireNonNull(duration, "duration");
        if (nonNullDuration.isNegative()) {
            throw new IllegalArgumentException("Duration must not be negative");
        }
        return nonNullDuration;
    }

    private static void requireFiniteNonNegative(double value, String label) {
        if (!Double.isFinite(value) || value < 0.0d) {
            throw new IllegalArgumentException(label + " must be finite and non-negative");
        }
    }
}
