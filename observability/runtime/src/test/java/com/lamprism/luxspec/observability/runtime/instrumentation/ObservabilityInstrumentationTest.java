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

import com.lamprism.luxspec.data.pagination.UnboundedWindow;
import com.lamprism.luxspec.data.query.InMemoryQueryExecutor;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.data.query.QueryField;
import com.lamprism.luxspec.observability.observation.ObservationHandler;
import com.lamprism.luxspec.observability.observation.ObservationRegistry;
import com.lamprism.luxspec.observability.runtime.observation.DatabaseObservationAttributes;
import com.lamprism.luxspec.observability.runtime.observation.ObservationRegistryBuilder;
import com.lamprism.luxspec.observability.runtime.observation.StandardObservationSet;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author RollW
 */
class ObservabilityInstrumentationTest {
    private static final QueryField<String> NAME = QueryField.of("name", String.class);

    @Test
    void instrumentsExecutorAndQueryBoundariesThroughTheSameObservationRegistry() {
        AtomicInteger stopped = new AtomicInteger();
        try (ObservationRegistry registry = registry(stopped)) {
            Executor executor = ObservabilityInstrumentation.executor(Runnable::run, registry);
            executor.execute(() -> {
            });

            InMemoryQueryExecutor<Item> queryExecutor = InMemoryQueryExecutor.<Item>builder(
                            () -> List.of(new Item("alpha"))
                    )
                    .field(NAME, Item::name)
                    .build();
            ObservabilityInstrumentation.queryExecutor(queryExecutor, registry)
                    .query(QueryCriteria.empty(), UnboundedWindow.getInstance());

            assertEquals(2, stopped.get());
        }
    }

    @Test
    void instrumentsJdbcConnectionsAndStatementsWithoutChangingTheDataSourceContract() throws Exception {
        AtomicInteger stopped = new AtomicInteger();
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:luxspec-observability;DB_CLOSE_DELAY=-1");
        try (ObservationRegistry registry = registry(stopped);
             Connection connection = ObservabilityInstrumentation.dataSource(source, registry)
                     .getConnection()) {
            connection.createStatement().execute("select 1");

            assertEquals(2, stopped.get());
        }
    }

    @Test
    void reportsOnlyBoundedDatabaseOperationKinds() throws Exception {
        List<DatabaseObservationAttributes.Operation> operations = new ArrayList<>();
        List<Integer> attributeCounts = new ArrayList<>();
        ObservationHandler handler = new ObservationHandler() {
            @Override
            public void onStop(com.lamprism.luxspec.observability.observation.ObservationView view) {
                operations.add(view.attributes().get(DatabaseObservationAttributes.OPERATION).orElseThrow());
                attributeCounts.add(view.attributes().values().size());
            }
        };
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:luxspec-operation-kinds;DB_CLOSE_DELAY=-1");
        try (ObservationRegistry registry = ObservationRegistryBuilder.builder()
                .set(StandardObservationSet.builder().domain(StandardObservationSet.Domain.DATABASE).build())
                .handler(handler).build();
             Connection connection = ObservabilityInstrumentation.dataSource(source, registry).getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("create table sample (id integer)");
            statement.executeUpdate("insert into sample values (1)");
            statement.executeQuery("select id from sample").close();
        }
        assertEquals(List.of(
                DatabaseObservationAttributes.Operation.CONNECT,
                DatabaseObservationAttributes.Operation.EXECUTE,
                DatabaseObservationAttributes.Operation.UPDATE,
                DatabaseObservationAttributes.Operation.QUERY
        ), operations);
        assertEquals(List.of(1, 1, 1, 1), attributeCounts);
    }

    private static ObservationRegistry registry(AtomicInteger stopped) {
        ObservationRegistryBuilder builder = ObservationRegistryBuilder.builder()
                .set(StandardObservationSet.builder().domain(StandardObservationSet.Domain.DATABASE).build())
                .set(StandardObservationSet.builder().domain(StandardObservationSet.Domain.INFRASTRUCTURE).build())
                .handler(new ObservationHandler() {
                    @Override
                    public void onStop(com.lamprism.luxspec.observability.observation.ObservationView view) {
                        stopped.incrementAndGet();
                    }
                });
        return builder.build();
    }

    private record Item(String name) {
    }
}
