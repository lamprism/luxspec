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

import com.lamprism.luxspec.data.pagination.QueryResult;
import com.lamprism.luxspec.data.pagination.QueryWindow;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.data.query.QueryExecutor;
import com.lamprism.luxspec.data.query.QueryPlan;
import com.lamprism.luxspec.observability.observation.Observation;
import com.lamprism.luxspec.observability.observation.ObservationOutcome;
import com.lamprism.luxspec.observability.observation.ObservationRegistry;
import com.lamprism.luxspec.observability.observation.ObservationSpec;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Instruments ordinary, projection, and time-series query execution with one observation role.
 *
 * <p>The wrapper is provider-independent, so the same adapter can instrument an in-memory or JPA
 * executor without changing the query contract.</p>
 *
 * @param <T> the source candidate or entity type
 * @author RollW
 */
public final class ObservedQueryExecutor<T>
        implements QueryExecutor<T> {
    private final QueryExecutor<T> delegate;
    private final ObservationRegistry registry;
    private final ObservationSpec spec;

    /**
     * Creates an observed query executor.
     *
     * @param delegate the query executor to call
     * @param registry the observation registry
     * @param spec     the registered observation specification
     */
    public ObservedQueryExecutor(
            QueryExecutor<T> delegate,
            ObservationRegistry registry,
            ObservationSpec spec
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.spec = Objects.requireNonNull(spec, "spec");
    }

    @Override
    public QueryResult<T> query(QueryCriteria criteria, QueryWindow window) {
        return observe(() -> delegate.query(criteria, window));
    }

    @Override
    public <R> QueryResult<R> query(QueryPlan<R> plan, QueryWindow window) {
        return observe(() -> delegate.query(plan, window));
    }

    private <R> R observe(Supplier<R> operation) {
        Observation observation = registry.start(spec);
        try {
            R result = operation.get();
            observation.setOutcome(ObservationOutcome.SUCCESS);
            return result;
        } catch (RuntimeException | Error failure) {
            observation.error(failure);
            throw failure;
        } finally {
            observation.stop();
        }
    }
}
