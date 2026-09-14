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

import com.lamprism.luxspec.observability.metric.Counter;
import com.lamprism.luxspec.observability.metric.DistributionSummary;
import com.lamprism.luxspec.observability.metric.FunctionCounter;
import com.lamprism.luxspec.observability.metric.FunctionTimer;
import com.lamprism.luxspec.observability.metric.Gauge;
import com.lamprism.luxspec.observability.metric.LongTaskTimer;
import com.lamprism.luxspec.observability.metric.Metric;
import com.lamprism.luxspec.observability.metric.MetricBinding;
import com.lamprism.luxspec.observability.metric.MetricDescription;
import com.lamprism.luxspec.observability.metric.MetricRegistry;
import com.lamprism.luxspec.observability.metric.MetricSpec;
import com.lamprism.luxspec.observability.metric.MetricUnit;
import com.lamprism.luxspec.observability.metric.TimeGauge;
import com.lamprism.luxspec.observability.metric.Timer;
import com.lamprism.luxspec.observability.runtime.metric.MetricRegistryEventSource;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Projects materialized Luxspec metrics into a Micrometer registry.
 *
 * @author RollW
 */
public class MicrometerMetricBridge implements AutoCloseable {
    private final MeterRegistry meterRegistry;
    private final MetricRegistryEventSource eventSource;
    private final Map<MetricBinding<?>, List<Meter>> projected = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lifecycleLock = new ReentrantReadWriteLock();
    private final AutoCloseable listener;
    private boolean closed;

    public MicrometerMetricBridge(
            MetricRegistry metricRegistry,
            MeterRegistry meterRegistry
    ) {
        Objects.requireNonNull(metricRegistry, "metricRegistry");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
        if (!(metricRegistry instanceof MetricRegistryEventSource registryEventSource)) {
            throw new IllegalArgumentException("The metric registry does not expose materialization events");
        }
        eventSource = registryEventSource;
        AutoCloseable registeredListener;
        try {
            registeredListener = eventSource.addMaterializationListener(this::project);
        } catch (RuntimeException failure) {
            removeProjectedMeters();
            throw failure;
        }
        listener = registeredListener;
    }

    @Override
    public void close() {
        lifecycleLock.writeLock().lock();
        try {
            if (closed) {
                return;
            }
            closed = true;
            try {
                listener.close();
            } catch (Exception failure) {
                throw new MicrometerProjectionException("Unable to detach the Micrometer metric bridge", failure);
            } finally {
                projected.clear();
            }
        } finally {
            lifecycleLock.writeLock().unlock();
        }
    }

    private void project(Metric metric) {
        lifecycleLock.readLock().lock();
        try {
            if (closed) {
                return;
            }
            MetricBinding<?> binding = metric.getBinding();
            projected.computeIfAbsent(binding, ignored -> projectMeters(binding, metric));
        } finally {
            lifecycleLock.readLock().unlock();
        }
    }

    private List<Meter> projectMeters(MetricBinding<?> binding, Metric metric) {
        List<Meter> meters = new ArrayList<>();
        try {
            if (metric instanceof Counter counter) {
                meters.add(registerFunctionCounter(binding, counter, binding.getSpec().getName().value()));
            } else if (metric instanceof Gauge gauge) {
                meters.add(registerGauge(binding, gauge, binding.getSpec().getName().value(), () -> {
                    Double value = gauge.value();
                    return value == null ? Double.NaN : value;
                }));
            } else if (metric instanceof FunctionCounter functionCounter) {
                meters.add(registerFunctionCounter(binding, functionCounter, binding.getSpec().getName().value()));
            } else if (metric instanceof Timer timer) {
                meters.add(registerTimer(binding, timer));
            } else if (metric instanceof DistributionSummary summary) {
                meters.add(registerGauge(
                        binding,
                        summary,
                        binding.getSpec().getName().value() + ".count",
                        () -> (double) summary.count()
                ));
                meters.add(registerGauge(
                        binding,
                        summary,
                        binding.getSpec().getName().value() + ".sum",
                        summary::totalAmount
                ));
                meters.add(registerGauge(
                        binding,
                        summary,
                        binding.getSpec().getName().value() + ".max",
                        () -> nullableDouble(summary.max())
                ));
            } else if (metric instanceof LongTaskTimer longTaskTimer) {
                meters.add(registerLongTaskTimer(binding, longTaskTimer));
            } else if (metric instanceof FunctionTimer functionTimer) {
                meters.add(registerGauge(
                        binding,
                        functionTimer,
                        binding.getSpec().getName().value() + ".count",
                        () -> nullableDouble(functionTimer.count())
                ));
                meters.add(registerGauge(
                        binding,
                        functionTimer,
                        binding.getSpec().getName().value() + ".total",
                        () -> nullableDuration(functionTimer.totalTime())
                ));
            } else if (metric instanceof TimeGauge timeGauge) {
                meters.add(registerGauge(
                        binding,
                        timeGauge,
                        binding.getSpec().getName().value(),
                        () -> nullableDuration(timeGauge.value())
                ));
            } else {
                throw new MicrometerProjectionException("Unsupported Luxspec metric: " + metric.getClass().getName());
            }
            return List.copyOf(meters);
        } catch (RuntimeException failure) {
            for (Meter meter : meters) {
                meterRegistry.remove(meter);
            }
            if (failure instanceof MicrometerProjectionException projectionFailure) {
                throw projectionFailure;
            }
            throw new MicrometerProjectionException(
                    "Unable to project metric " + binding.getSpec().getName(),
                    failure
            );
        }
    }

    private Meter registerFunctionCounter(
            MetricBinding<?> binding,
            Object source,
            String name
    ) {
        return register(name, binding, () -> {
            io.micrometer.core.instrument.FunctionCounter.Builder<Object> builder =
                    io.micrometer.core.instrument.FunctionCounter.builder(
                            name,
                            source,
                            value -> numericValue(source, value)
                    );
            configure(builder, binding.getSpec());
            return builder.tags(tags(binding)).register(meterRegistry);
        });
    }

    private Meter registerGauge(
            MetricBinding<?> binding,
            Object source,
            String name,
            Supplier<Double> reader
    ) {
        return register(name, binding, () -> {
            io.micrometer.core.instrument.Gauge.Builder<Object> builder =
                    io.micrometer.core.instrument.Gauge.builder(name, source, ignored -> reader.get());
            configure(builder, binding.getSpec());
            return builder.tags(tags(binding)).register(meterRegistry);
        });
    }

    private Meter registerTimer(MetricBinding<?> binding, Timer timer) {
        return register(binding.getSpec().getName().value(), binding, () -> {
            io.micrometer.core.instrument.FunctionTimer.Builder<Timer> builder =
                    io.micrometer.core.instrument.FunctionTimer.builder(
                            binding.getSpec().getName().value(),
                            timer,
                            Timer::count,
                            value -> seconds(value.totalTime()),
                            TimeUnit.SECONDS
                    );
            configure(builder, binding.getSpec());
            return builder.tags(tags(binding)).register(meterRegistry);
        });
    }

    private Meter registerLongTaskTimer(MetricBinding<?> binding, LongTaskTimer timer) {
        return register(binding.getSpec().getName().value(), binding, () -> {
            checkedActiveTasks(timer);
            List<Measurement> measurements = List.of(
                    new Measurement(
                            () -> (double) checkedActiveTasks(timer),
                            Statistic.ACTIVE_TASKS
                    ),
                    new Measurement(
                            () -> seconds(timer.activeDuration()),
                            Statistic.DURATION
                    )
            );
            Meter.Builder builder = Meter.builder(
                    binding.getSpec().getName().value(),
                    Meter.Type.LONG_TASK_TIMER,
                    measurements
            );
            configure(builder, binding.getSpec());
            return builder.tags(tags(binding)).register(meterRegistry);
        });
    }

    private Meter register(String name, MetricBinding<?> binding, Supplier<Meter> factory) {
        Tags expectedTags = Tags.of(tags(binding));
        for (Meter existing : meterRegistry.getMeters()) {
            if (existing.getId().getName().equals(name) && existing.getId().getTags().equals(expectedTags)) {
                throw new MicrometerProjectionException("Micrometer meter already exists: " + existing.getId());
            }
        }
        return factory.get();
    }

    private static List<Tag> tags(MetricBinding<?> binding) {
        List<Tag> tags = new ArrayList<>();
        for (Map.Entry<String, String> dimension : binding.getDimensions().entrySet()) {
            tags.add(Tag.of(dimension.getKey(), dimension.getValue()));
        }
        return List.copyOf(tags);
    }

    private static void configure(Object builder, MetricSpec<?> spec) {
        MetricDescription description = spec.getDescription();
        MetricUnit baseUnit = spec.getBaseUnit();
        if (builder instanceof io.micrometer.core.instrument.FunctionCounter.Builder<?> functionCounterBuilder) {
            if (description != null) {
                functionCounterBuilder.description(description.value());
            }
            if (baseUnit != null) {
                functionCounterBuilder.baseUnit(baseUnit.value());
            }
        } else if (builder instanceof io.micrometer.core.instrument.Gauge.Builder<?> gaugeBuilder) {
            if (description != null) {
                gaugeBuilder.description(description.value());
            }
            if (baseUnit != null) {
                gaugeBuilder.baseUnit(baseUnit.value());
            }
        } else if (builder instanceof io.micrometer.core.instrument.FunctionTimer.Builder<?> functionTimerBuilder) {
            if (description != null) {
                functionTimerBuilder.description(description.value());
            }
        } else if (builder instanceof io.micrometer.core.instrument.Meter.Builder meterBuilder) {
            if (description != null) {
                meterBuilder.description(description.value());
            }
            if (baseUnit != null) {
                meterBuilder.baseUnit(baseUnit.value());
            }
        }
    }

    private static double numericValue(Object source, Object value) {
        if (source instanceof Counter counter) {
            return counter.count();
        }
        if (source instanceof FunctionCounter functionCounter) {
            Double count = functionCounter.count();
            return count == null ? Double.NaN : count;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.NaN;
    }

    private static double nullableDouble(@Nullable Number value) {
        return value == null ? Double.NaN : value.doubleValue();
    }

    private static double nullableDuration(@Nullable Duration value) {
        return value == null ? Double.NaN : seconds(value);
    }

    private static double seconds(Duration value) {
        return value.getSeconds() + value.getNano() / 1_000_000_000.0d;
    }

    private static int checkedActiveTasks(LongTaskTimer timer) {
        try {
            return Math.toIntExact(timer.activeTasks());
        } catch (ArithmeticException failure) {
            throw new MicrometerProjectionException("LongTaskTimer active task count exceeds Micrometer range", failure);
        }
    }

    private void removeProjectedMeters() {
        for (List<Meter> meters : projected.values()) {
            for (Meter meter : meters) {
                meterRegistry.remove(meter);
            }
        }
        projected.clear();
    }
}
