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

package com.lamprism.luxspec.observability.runtime.instrumentation;

import com.lamprism.luxspec.observability.observation.Observation;
import com.lamprism.luxspec.observability.observation.ObservationOutcome;
import com.lamprism.luxspec.observability.observation.ObservationRegistry;
import com.lamprism.luxspec.observability.observation.ObservationSpec;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author RollW
 */
final class ObservationExecutors {
    private ObservationExecutors() {
    }

    static Executor executor(
            Executor delegate,
            ObservationRegistry registry,
            ObservationSpec spec
    ) {
        return new ObservedExecutor(delegate, registry, spec);
    }

    static ScheduledExecutorService scheduledExecutor(
            ScheduledExecutorService delegate,
            ObservationRegistry registry,
            ObservationSpec spec
    ) {
        return new ObservedScheduledExecutor(delegate, registry, spec);
    }

    private static Runnable observed(
            Runnable command,
            ObservationRegistry registry,
            ObservationSpec spec
    ) {
        Runnable nonNullCommand = Objects.requireNonNull(command, "command");
        return () -> {
            Observation observation = registry.start(spec);
            try {
                nonNullCommand.run();
                observation.setOutcome(ObservationOutcome.SUCCESS);
            } catch (RuntimeException | Error failure) {
                observation.error(failure);
                throw failure;
            } finally {
                observation.stop();
            }
        };
    }

    private static <V> Callable<V> observed(
            Callable<V> command,
            ObservationRegistry registry,
            ObservationSpec spec
    ) {
        Callable<V> nonNullCommand = Objects.requireNonNull(command, "command");
        return () -> {
            Observation observation = registry.start(spec);
            try {
                V result = nonNullCommand.call();
                observation.setOutcome(ObservationOutcome.SUCCESS);
                return result;
            } catch (Exception | Error failure) {
                observation.error(failure);
                throw failure;
            } finally {
                observation.stop();
            }
        };
    }

    private static class ObservedExecutor implements Executor {
        private final Executor delegate;
        private final ObservationRegistry registry;
        private final ObservationSpec spec;

        private ObservedExecutor(
                Executor delegate,
                ObservationRegistry registry,
                ObservationSpec spec
        ) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
            this.registry = Objects.requireNonNull(registry, "registry");
            this.spec = Objects.requireNonNull(spec, "spec");
        }

        @Override
        public void execute(Runnable command) {
            delegate.execute(observed(command, registry, spec));
        }
    }

    private static final class ObservedScheduledExecutor
            extends AbstractExecutorService
            implements ScheduledExecutorService {
        private final ScheduledExecutorService delegate;
        private final ObservationRegistry registry;
        private final ObservationSpec spec;

        private ObservedScheduledExecutor(
                ScheduledExecutorService delegate,
                ObservationRegistry registry,
                ObservationSpec spec
        ) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
            this.registry = Objects.requireNonNull(registry, "registry");
            this.spec = Objects.requireNonNull(spec, "spec");
        }

        @Override
        public void execute(Runnable command) {
            delegate.execute(observed(command, registry, spec));
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            return delegate.schedule(observed(command, registry, spec), delay, unit);
        }

        @Override
        public <V> ScheduledFuture<V> schedule(
                Callable<V> command,
                long delay,
                TimeUnit unit
        ) {
            return delegate.schedule(observed(command, registry, spec), delay, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(
                Runnable command,
                long initialDelay,
                long period,
                TimeUnit unit
        ) {
            return delegate.scheduleAtFixedRate(
                    observed(command, registry, spec),
                    initialDelay,
                    period,
                    unit
            );
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(
                Runnable command,
                long initialDelay,
                long delay,
                TimeUnit unit
        ) {
            return delegate.scheduleWithFixedDelay(
                    observed(command, registry, spec),
                    initialDelay,
                    delay,
                    unit
            );
        }
    }
}
