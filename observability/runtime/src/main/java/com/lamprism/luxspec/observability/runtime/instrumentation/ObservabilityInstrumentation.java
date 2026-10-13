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

import com.lamprism.luxspec.data.query.QueryExecutor;
import com.lamprism.luxspec.observability.observation.ObservationRegistry;
import com.lamprism.luxspec.observability.observation.ObservationSpec;
import com.lamprism.luxspec.observability.runtime.observation.StandardObservationCatalog;

import javax.sql.DataSource;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Single entry point for the provider-independent infrastructure instrumentation adapters.
 *
 * <p>The returned wrappers do not own or close the supplied delegate. Applications can select
 * the standard Luxspec observation declarations or provide a registered custom specification.</p>
 *
 * <p>Spring Boot auto-configuration can apply these wrappers to explicitly named infrastructure
 * beans. Broad type-based wrapping remains disabled because delegate ownership, replacement
 * order, and duplicate instrumentation belong to the application that assembled the dependency.</p>
 *
 * @author RollW
 */
public final class ObservabilityInstrumentation {
    private ObservabilityInstrumentation() {
    }

    /**
     * Instruments query execution with the standard database observation.
     *
     * @param delegate the query executor
     * @param registry the observation registry
     * @param <T>      the source type
     * @return the observed executor
     */
    public static <T> ObservedQueryExecutor<T> queryExecutor(
            QueryExecutor<T> delegate,
            ObservationRegistry registry
    ) {
        return queryExecutor(delegate, registry, StandardObservationCatalog.DATABASE_OPERATION);
    }

    /**
     * Instruments query execution with an application-selected observation.
     *
     * @param delegate the query executor
     * @param registry the observation registry
     * @param spec     the registered observation specification
     * @param <T>      the source type
     * @return the observed executor
     */
    public static <T> ObservedQueryExecutor<T> queryExecutor(
            QueryExecutor<T> delegate,
            ObservationRegistry registry,
            ObservationSpec spec
    ) {
        return new ObservedQueryExecutor<>(delegate, registry, spec);
    }

    /**
     * Instruments JDBC connection and statement operations with the standard database
     * observation.
     *
     * @param dataSource the application-owned data source
     * @param registry   the observation registry
     * @return the observed data source
     */
    public static DataSource dataSource(DataSource dataSource, ObservationRegistry registry) {
        return dataSource(dataSource, registry, StandardObservationCatalog.DATABASE_OPERATION);
    }

    /**
     * Instruments JDBC connection and statement operations with an application-selected
     * observation.
     *
     * @param dataSource the application-owned data source
     * @param registry   the observation registry
     * @param spec       the registered observation specification
     * @return the observed data source
     */
    public static DataSource dataSource(
            DataSource dataSource,
            ObservationRegistry registry,
            ObservationSpec spec
    ) {
        return new ObservedDataSource(dataSource, registry, spec);
    }

    /**
     * Instruments submitted tasks with the standard executor observation.
     *
     * @param delegate the application-owned executor
     * @param registry the observation registry
     * @return the observed executor
     */
    public static Executor executor(Executor delegate, ObservationRegistry registry) {
        return executor(delegate, registry, StandardObservationCatalog.EXECUTOR_TASK);
    }

    /**
     * Instruments submitted tasks with an application-selected observation.
     *
     * @param delegate the application-owned executor
     * @param registry the observation registry
     * @param spec     the registered observation specification
     * @return the observed executor
     */
    public static Executor executor(
            Executor delegate,
            ObservationRegistry registry,
            ObservationSpec spec
    ) {
        return ObservationExecutors.executor(delegate, registry, spec);
    }

    /**
     * Instruments scheduled tasks with the standard executor observation.
     *
     * @param delegate the application-owned scheduler
     * @param registry the observation registry
     * @return the observed scheduler
     */
    public static ScheduledExecutorService scheduledExecutor(
            ScheduledExecutorService delegate,
            ObservationRegistry registry
    ) {
        return scheduledExecutor(delegate, registry, StandardObservationCatalog.EXECUTOR_TASK);
    }

    /**
     * Instruments scheduled tasks with an application-selected observation.
     *
     * @param delegate the application-owned scheduler
     * @param registry the observation registry
     * @param spec     the registered observation specification
     * @return the observed scheduler
     */
    public static ScheduledExecutorService scheduledExecutor(
            ScheduledExecutorService delegate,
            ObservationRegistry registry,
            ObservationSpec spec
    ) {
        return ObservationExecutors.scheduledExecutor(delegate, registry, spec);
    }
}
