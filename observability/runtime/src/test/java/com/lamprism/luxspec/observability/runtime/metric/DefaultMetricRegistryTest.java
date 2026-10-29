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

import com.lamprism.luxspec.observability.metric.GaugeSpec;
import com.lamprism.luxspec.observability.metric.LongTaskTimer;
import com.lamprism.luxspec.observability.metric.LongTaskTimerSpec;
import com.lamprism.luxspec.observability.metric.LongTaskTiming;
import com.lamprism.luxspec.observability.metric.MetricDimensionSet;
import com.lamprism.luxspec.observability.metric.MetricDimensionSpec;
import com.lamprism.luxspec.observability.metric.MetricRegistry;
import com.lamprism.luxspec.observability.metric.NumberReader;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author RollW
 */
class DefaultMetricRegistryTest {
    @Test
    void rejectsMetricDefinitionsWhoseSameClassReaderHasDifferentCapturedState() {
        GaugeSpec<AtomicValue> first = GaugeSpec.builder("worker.load", AtomicValue.class)
                .reader(new ScaleReader(2))
                .build();
        GaugeSpec<AtomicValue> second = GaugeSpec.builder("worker.load", AtomicValue.class)
                .reader(new ScaleReader(3))
                .build();

        assertNotEquals(first, second);
        try (MetricRegistry registry = MetricRegistryBuilder.builder().build()) {
            registry.register(first);
            assertThrows(IllegalArgumentException.class, () -> registry.register(second));
        }
    }

    @Test
    void distinguishesDimensionFormattersWithDifferentCapturedState() {
        MetricDimensionSpec<String> first = MetricDimensionSpec.builder("route", String.class)
                .formatter(new PrefixFormatter("first:"))
                .build();
        MetricDimensionSpec<String> second = MetricDimensionSpec.builder("route", String.class)
                .formatter(new PrefixFormatter("second:"))
                .build();

        assertNotEquals(first, second);
    }

    @Test
    void startsLongTaskWhileSnapshotReadsAnotherMetric() throws Exception {
        BlockingGaugeSource source = new BlockingGaugeSource();
        GaugeSpec<BlockingGaugeSource> gaugeSpec = GaugeSpec
                .builder("snapshot.blocker", BlockingGaugeSource.class)
                .reader(BlockingGaugeSource::read)
                .build();
        LongTaskTimerSpec timerSpec = LongTaskTimerSpec.builder("long.task").build();

        try (MetricRegistry registry = MetricRegistryBuilder.builder().build()) {
            registry.register(gaugeSpec);
            registry.register(timerSpec);
            registry.obtain(gaugeSpec.bind(MetricDimensionSet.empty(), source));
            LongTaskTimer timer = registry.obtain(timerSpec);

            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<?> snapshot = executor.submit(registry::snapshot);
                source.awaitReadStarted();

                Future<?> start = executor.submit(() -> {
                    try (LongTaskTiming ignored = timer.start()) {
                    }
                });
                start.get(1L, TimeUnit.SECONDS);

                source.releaseRead();
                snapshot.get(1L, TimeUnit.SECONDS);
            } finally {
                source.releaseRead();
                executor.shutdownNow();
                assertTrue(executor.awaitTermination(5L, TimeUnit.SECONDS));
            }
        }
    }

    private static final class BlockingGaugeSource {
        private final CountDownLatch readStarted = new CountDownLatch(1);
        private final CountDownLatch releaseRead = new CountDownLatch(1);

        private Number read() {
            readStarted.countDown();
            try {
                if (!releaseRead.await(5L, TimeUnit.SECONDS)) {
                    throw new AssertionError("Timed out waiting for the snapshot reader");
                }
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Snapshot reader was interrupted", failure);
            }
            return 1.0d;
        }

        private void awaitReadStarted() throws InterruptedException {
            assertTrue(readStarted.await(5L, TimeUnit.SECONDS));
        }

        private void releaseRead() {
            releaseRead.countDown();
        }
    }

    private static final class AtomicValue {
    }

    private static final class ScaleReader implements NumberReader<AtomicValue> {
        private final int scale;

        private ScaleReader(int scale) {
            this.scale = scale;
        }

        @Override
        public Number read(AtomicValue source) {
            return scale;
        }
    }

    private static final class PrefixFormatter implements Function<String, String> {
        private final String prefix;

        private PrefixFormatter(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public String apply(String value) {
            return prefix + value;
        }
    }
}
