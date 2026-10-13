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

package com.lamprism.luxspec.observability.autoconfigure;

import com.lamprism.luxspec.data.query.QueryExecutor;
import com.lamprism.luxspec.observability.observation.ObservationRegistry;
import com.lamprism.luxspec.observability.observation.ObservationSpec;
import com.lamprism.luxspec.observability.runtime.instrumentation.ObservabilityInstrumentation;
import com.lamprism.luxspec.observability.runtime.instrumentation.ObservedQueryExecutor;
import com.lamprism.luxspec.observability.runtime.observation.StandardObservationCatalog;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Applies infrastructure observation wrappers to explicitly selected Spring beans.
 *
 * <p>The post-processor does not scan all beans by type. A wrapper changes the runtime object
 * returned for a bean, so selection remains an explicit configuration decision made by the
 * application.</p>
 *
 * @author RollW
 */
final class LuxspecObservabilityInstrumentationPostProcessor implements BeanPostProcessor {
    private final ObservationRegistry registry;
    private final Set<String> dataSourceBeans;
    private final Set<String> executorBeans;
    private final Set<String> queryExecutorBeans;
    private final ObservationSpec databaseSpec;
    private final ObservationSpec executorSpec;

    LuxspecObservabilityInstrumentationPostProcessor(
            LuxspecObservabilityConfiguration settings,
            ObservationRegistry registry
    ) {
        this.registry = Objects.requireNonNull(registry, "registry");
        LuxspecObservabilityConfiguration.InstrumentationConfiguration instrumentation =
                Objects.requireNonNull(settings, "settings").getInstrumentation();
        dataSourceBeans = instrumentation.getDataSourceBeans();
        executorBeans = instrumentation.getExecutorBeans();
        queryExecutorBeans = instrumentation.getQueryExecutorBeans();
        databaseSpec = findRequiredSpec(
                StandardObservationCatalog.DATABASE_OPERATION,
                !dataSourceBeans.isEmpty() || !queryExecutorBeans.isEmpty(),
                "data-source-beans or query-executor-beans"
        );
        executorSpec = findRequiredSpec(
                StandardObservationCatalog.EXECUTOR_TASK,
                !executorBeans.isEmpty(),
                "executor-beans"
        );
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (dataSourceBeans.contains(beanName)) {
            return wrapDataSource(bean, beanName);
        }
        if (executorBeans.contains(beanName)) {
            return wrapExecutor(bean, beanName);
        }
        if (queryExecutorBeans.contains(beanName)) {
            return wrapQueryExecutor(bean, beanName);
        }
        return bean;
    }

    private Object wrapDataSource(Object bean, String beanName) {
        if (!(bean instanceof DataSource dataSource)) {
            throw wrongType(beanName, DataSource.class, bean);
        }
        return ObservabilityInstrumentation.dataSource(dataSource, registry, databaseSpec);
    }

    private Object wrapExecutor(Object bean, String beanName) {
        if (!(bean instanceof Executor executor)) {
            throw wrongType(beanName, Executor.class, bean);
        }
        if (executor instanceof ScheduledExecutorService scheduledExecutor) {
            return ObservabilityInstrumentation.scheduledExecutor(scheduledExecutor, registry, executorSpec);
        }
        return ObservabilityInstrumentation.executor(executor, registry, executorSpec);
    }

    private Object wrapQueryExecutor(Object bean, String beanName) {
        if (!(bean instanceof QueryExecutor<?> queryExecutor)) {
            throw wrongType(beanName, QueryExecutor.class, bean);
        }
        if (queryExecutor instanceof ObservedQueryExecutor<?>) {
            return bean;
        }
        return wrapQueryExecutorUnchecked(queryExecutor);
    }

    private <T> ObservedQueryExecutor<T> wrapQueryExecutorUnchecked(QueryExecutor<T> queryExecutor) {
        return ObservabilityInstrumentation.queryExecutor(queryExecutor, registry, databaseSpec);
    }

    private ObservationSpec findRequiredSpec(
            ObservationSpec standardSpec,
            boolean required,
            String propertyName
    ) {
        if (!required) {
            return standardSpec;
        }
        ObservationSpec registered = registry.find(standardSpec.getName());
        if (registered == null) {
            throw new IllegalStateException(
                    "Observation instrumentation property " + propertyName
                            + " requires the corresponding standard observation declaration"
            );
        }
        return registered;
    }

    private static BeanCreationException wrongType(String beanName, Class<?> expected, Object actual) {
        return new BeanCreationException(
                beanName,
                "Bean selected for observability instrumentation must implement "
                        + expected.getName() + ", but was " + actual.getClass().getName()
        );
    }
}
